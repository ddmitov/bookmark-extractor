package main.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class BookmarkExtractor {
    public static int connectTimeout = 5000;
    public static int readTimeout = 5000;

    public static boolean statusCheck = true;
    public static String targetFolderName = "";
    public static int matchingFolders = 0;

    public static String bookmarksPath;
    public static String outputPath;
    public static String errorsPath;

    public static PrintWriter outputWriter;
    public static PrintWriter errorWriter;

    public static void main(String args[]) {
        try {
            // Command-line arguments:
            if (args.length == 1 && args[0].matches("--help")) {
                printHeader();
                System.out.println("java -jar bookmark-extractor.jar --root=root_folder");
                System.out.println("java -jar bookmark-extractor.jar --root=\"root folder with spaces\"");
                System.out.println("");
                System.out.println("Arguments:");
                System.out.println("--help                this help");
                System.out.println("--root=<node-name>    root node - mandatory argument");
                System.out.println("--no-check            do not check URLs");
                System.out.println("");
                System.exit(1);
            }

            if (args.length > 0) {
                for (int index = 0; index < args.length; index++) {
                    if (args[index].matches("--no-check")) {
                        statusCheck = false;
                    }

                    if (args[index].contains("--root")) {
                        targetFolderName = args[index].replace("--root=", "");
                    }
                }
            }

            if (targetFolderName.length() == 0) {
                printHeader();
                System.out.println("No root element is specified!");
                System.exit(1);
            }

            // Bookmarks path:
            bookmarksPath = System.getProperty("user.home")
                    + "/.config/chromium/Default/Bookmarks";

            File bookmarksFile = new File(bookmarksPath);
            if (!bookmarksFile.exists()) {
                printHeader();
                System.out.println("Bookmarks file is not found!");
                System.exit(1);
            }

            System.out.println("Bookmark Extractor is working.");
            System.out.println("Only problematic adresses are going to be displayed.");
            System.out.println("");

            // Output files:
            final String currentDirectory = System.getProperty("user.dir");
            outputPath = currentDirectory + "/bookmarks.md";
            errorsPath = currentDirectory + "/bookmarks-with-errors.txt";

            outputWriter = new PrintWriter(new FileWriter(outputPath));
            errorWriter = new PrintWriter(new FileWriter(errorsPath));

            outputWriter.println("## " + targetFolderName);
            outputWriter.flush();

            // Bookmarks JSON parsing:
            BufferedReader buffer =
                    new BufferedReader(new FileReader(bookmarksPath));

            Gson gson = new Gson();
            JsonObject bookmarks = gson.fromJson(buffer, JsonObject.class);
            JsonObject other = bookmarks
                    .getAsJsonObject("roots").getAsJsonObject("other");

            findTargetJsonNodes("", other);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        outputWriter.println("");
        outputWriter.println("Created using [Bookmark Extractor](https://github.com/ddmitov/bookmark-extractor)");
        outputWriter.println("");
        outputWriter.flush();

        System.out.println("");
        System.out.println("Bookmark Extractor ended its job successfully!");
    }

    public static void findTargetJsonNodes(
            String nodeName,
            JsonObject node)
                    throws IOException {

        JsonArray children = node.getAsJsonArray("children");

        for (JsonElement child : children) {
            JsonObject childNode = child.getAsJsonObject();
            String childNodeType = childNode.get("type").getAsString();
            String childNodeName = childNode.get("name").getAsString();

            if (childNodeType.equals("folder")) {
                if (childNodeName.contains(targetFolderName)) {
                    matchingFolders++;
                    if (matchingFolders > 0) {
                        parseJsonNode(nodeName, childNodeName, childNode, 0);
                    } else {
                        parseJsonNode("", "", childNode, 0);
                    }
                }

                if (!childNodeName.contains(targetFolderName)) {
                    findTargetJsonNodes(childNodeName, childNode);
                }
            }
        }
    }

    public static void parseJsonNode(
            String parentNodeName,
            String nodeName,
            JsonObject node,
            int nodeLevel)
                    throws IOException {

    	String space = "  ".repeat(nodeLevel);

        if (parentNodeName.length() > 0 && nodeName.length() > 0) {
            outputWriter.println(space + "* " + parentNodeName + "  ");
            outputWriter.flush();

            outputWriter.println(space + "* " + nodeName + "  ");
            outputWriter.flush();
        }

        JsonArray children = node.getAsJsonObject().getAsJsonArray("children");

        Map<String, String> unsortedUrlMap = new TreeMap<String, String>();
        Map<String, String> sortedUrlMap = new TreeMap<String, String>();

        Map<String, JsonObject> unsortedFolderMap =
                new TreeMap<String, JsonObject>();
        Map<String, JsonObject> sortedFolderMap =
                new TreeMap<String, JsonObject>();

        for (JsonElement childElement : children) {
            JsonObject targetChildObject = childElement.getAsJsonObject();

            String type = targetChildObject.get("type").getAsString();
            String name = targetChildObject.get("name").getAsString();
            String address = "";

            if (type.equals("folder")) {
                unsortedFolderMap.put(name, targetChildObject);
            }

            if (type.equals("url")) {
                address = targetChildObject.get("url").getAsString();
                unsortedUrlMap.put(name, address);
            }
        }

        sortedFolderMap.putAll(unsortedFolderMap);
        sortedUrlMap.putAll(unsortedUrlMap);

        for (Map.Entry<String, JsonObject> entry : sortedFolderMap.entrySet()) {
            String name = entry.getKey();

            outputWriter.println(space + "  * " + name + "  ");
            outputWriter.flush();

            parseJsonNode("", "", entry.getValue(), nodeLevel + 1);
        }

        for (Map.Entry<String, String> entry : sortedUrlMap.entrySet()) {
            String name = entry.getKey();
            String urlString = entry.getValue();

            if (statusCheck == true) {
                URL url = new URL(urlString);
                HttpURLConnection httpConnection =
                        (HttpURLConnection) url.openConnection();
                httpConnection.setRequestMethod("HEAD");
                httpConnection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 Firefox/3.5.2"
                );
                httpConnection.setConnectTimeout(connectTimeout);
                httpConnection.setReadTimeout(readTimeout);

                int responseCode = 0;
                try {
                    responseCode = httpConnection.getResponseCode();
                } catch (ConnectException connectionException) {
                    System.out.println(urlString + " :: connection exception");

                    errorWriter.println(urlString + " :: connection exception");
                    errorWriter.flush();
                } catch (SocketException timeoutException) {
                    System.out.println(urlString + " :: socket exception");

                    errorWriter.println(urlString + " :: socket exception");
                    errorWriter.flush();
                } catch (SocketTimeoutException timeoutException) {
                    System.out.println(urlString + " :: socket timeout");

                    errorWriter.println(urlString + " :: socket timeout");
                    errorWriter.flush();
                } catch (SSLHandshakeException sslHandshakeException) {
                    System.out.println(urlString + " :: SSL handshake exception");
                    errorWriter.println(urlString + " :: SSL handshake exception");
                    errorWriter.flush();
                } catch (SSLException sslException) {
                    System.out.println(urlString + " :: SSL exception");
                    errorWriter.println(urlString + " :: SSL exception");
                    errorWriter.flush();
                } catch (ProtocolException protocolException) {
                    System.out.println(urlString + " :: protocol exception");

                    errorWriter.println(urlString + " :: protocol exception");
                    errorWriter.flush();
                } catch (UnknownHostException unknownHostException) {
                    System.out.println(urlString + " :: unknown host exception");

                    errorWriter.println(urlString + " :: unknown host exception");
                    errorWriter.flush();
                }

                if (responseCode == 200 || responseCode == 301
                        || responseCode == 302 || responseCode == 406) {
                    outputWriter.println(space + "  * [" + name + "]("
                            + urlString + ")  ");
                    outputWriter.flush();
                } else {
                    if (responseCode > 0) {
                        System.out.println(urlString + " :: " + responseCode);

                        errorWriter.println(urlString + " :: " + responseCode);
                        errorWriter.flush();
                    }
                }
            }

            if (statusCheck == false) {
                System.out.println(space + name + " :: " + urlString);

                outputWriter.println(space + "* [" + name + "](" + urlString
                        + ")  ");
                outputWriter.flush();
            }
        }
    }

    public static void printHeader() {
        System.out.println("");
        System.out.println("Bookmark Extractor version 0.2");
        System.out.println("Selective bookmark extractor and formatter.");
        System.out.println("");
    }
}
