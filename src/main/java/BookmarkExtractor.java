package main.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;

import org.omg.CORBA.portable.UnknownException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class BookmarkExtractor {
    public static String bookmarksPath;
    public static String outputPath;
    public static String errorsPath;
    public static String targetRootName = "";
    public static boolean statusCheck = true;

    public static void main(String[] args) {
        try {
            // Command-line arguments:
            if (args.length == 1 && args[0].matches("--help")) {
                printHeader();
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
                        targetRootName = args[index].replace("--root=", "");
                    }
                }
            }

            if (targetRootName.length() == 0) {
                printHeader();
                System.out.println("No root element is specified!");
                System.exit(1);
            }

            // Bookmarks path:
            final String homeDirectory = System.getProperty("user.home");
            final String fileSeparator = System.getProperty("file.separator");
            final String linuxConfigDir = ".config";

            bookmarksPath = homeDirectory + fileSeparator + linuxConfigDir
                    + fileSeparator + "chromium/Default/Bookmarks";

            File bookmarksFile = new File(bookmarksPath);
            if (!bookmarksFile.exists()) {
                printHeader();
                System.out.println("Bookmarks file is not found!");
                System.exit(1);
            }

            // Output files:
            final String currentDirectory = System.getProperty("user.dir");
            outputPath = currentDirectory + fileSeparator + "bookmarks.md";
            errorsPath = currentDirectory + fileSeparator + "bookmark-errors.md";

            PrintWriter outputWriter = new PrintWriter(
                    new FileWriter(outputPath));
            PrintWriter errorWriter = new PrintWriter(
                    new FileWriter(errorsPath));

            outputWriter.println("## " + targetRootName);
            outputWriter.flush();

            errorWriter.println("## " + targetRootName);
            errorWriter.flush();

            outputWriter.close();
            errorWriter.close();

            // Bookmarks JSON parsing:
            BufferedReader buffer = new BufferedReader(new FileReader(
                    bookmarksPath));

            Gson gson = new Gson();
            JsonObject bookmarks = gson.fromJson(buffer, JsonObject.class);

            JsonObject root = bookmarks.getAsJsonObject("roots");
            JsonObject other = root.getAsJsonObject("other");
            JsonArray roots = other.getAsJsonArray("children");

            for (JsonElement child : roots) {
                JsonObject childObject = child.getAsJsonObject();
                String type = childObject.get("type").getAsString();
                String name = childObject.get("name").getAsString();

                if (type.equals("folder")) {
                    if (name.equals(targetRootName)) {
                        parseElement(child, 0);
                    } else {
                        findRootElement(child);
                    }
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void findRootElement(JsonElement element) throws IOException {

        JsonObject elementObject = element.getAsJsonObject();
        JsonArray roots = elementObject.getAsJsonArray("children");

        for (JsonElement child : roots) {
            JsonObject childObject = child.getAsJsonObject();
            String type = childObject.get("type").getAsString();
            String name = childObject.get("name").getAsString();

            if (type.equals("folder")) {
                if (name.equals(targetRootName)) {
                    parseElement(child, 1);
                } else {
                    findRootElement(child);
                }
            }
        }
    }

    public static void parseElement(JsonElement child, int elementLevel)
            throws IOException {
        PrintWriter outputWriter = new PrintWriter(new FileWriter(outputPath,
                true));
        PrintWriter errorWriter = new PrintWriter(new FileWriter(errorsPath,
                true));

        JsonObject targetRoot = child.getAsJsonObject();
        JsonArray children = targetRoot.getAsJsonArray("children");

        for (JsonElement childElement : children) {
            JsonObject targetChildObject = childElement.getAsJsonObject();
            String type = targetChildObject.get("type").getAsString();
            String name = targetChildObject.get("name").getAsString();

            String space = String
                    .join("", Collections.nCopies(elementLevel, "  "));

            if (type.equals("folder")) {
                System.out.println(space + name);

                outputWriter.println(space + "* " + name + "  ");
                outputWriter.flush();

                errorWriter.println(space + "* " + name + "  ");
                errorWriter.flush();

                parseElement(childElement, elementLevel + 1);
            }

            if (type.equals("url")) {
                String urlString = targetChildObject.get("url").getAsString();

                if (statusCheck == true) {
                    URL url = new URL(urlString);
                    HttpURLConnection httpConnection = (HttpURLConnection) url
                            .openConnection();
                    httpConnection.setRequestMethod("HEAD");
                    httpConnection.setRequestProperty("User-Agent",
                            "Mozilla/5.0 Firefox/3.5.2");
                    httpConnection.setConnectTimeout(30000);
                    httpConnection.setReadTimeout(30000);

                    int responseCode = 0;
                    try {
                        InputStream errors = httpConnection.getErrorStream();
                        if (errors == null) {
                            responseCode = httpConnection.getResponseCode();
                        }
                    } catch (ConnectException connectionException) {
                        System.out.println(name + " : " + urlString
                                + " :: connection exception");
                        errorWriter.println(space + "* [" + name + "]("
                                + urlString + ") :: connection exception  ");
                        errorWriter.flush();
                    } catch (SocketException timeoutException) {
                        System.out.println(name + " : " + urlString
                                + " :: socket exception");
                        errorWriter.println(space + "* [" + name + "]("
                                + urlString + ") :: socket exception  ");
                        errorWriter.flush();
                    } catch (SocketTimeoutException timeoutException) {
                        System.out.println(name + " : " + urlString
                                + " :: timed out");
                        errorWriter.println(space + "* [" + name + "]("
                                + urlString + ") :: timed out  ");
                        errorWriter.flush();
                    } catch (ProtocolException protocolException) {
                        System.out.println(name + " : " + urlString
                                + " :: protocol exception");
                        errorWriter.println(space + "* [" + name + "]("
                                + urlString + ") :: protocol exception  ");
                        errorWriter.flush();
                    } catch (UnknownHostException unknownHostException) {
                        System.out.println(name + " : " + urlString
                                + " :: unknown host exception");
                        errorWriter.println(space + "* [" + name + "]("
                                + urlString + ") :: unknown host exception  ");
                        errorWriter.flush();
                    } catch (UnknownException unknownException) {
                        System.out.println(name + " : " + urlString
                                + " :: unknown exception");
                        errorWriter.println(space + "* [" + name + "]("
                                + urlString + ") :: unknown exception  ");
                        errorWriter.flush();
                    }

                    if (responseCode == 200 || responseCode == 301
                            || responseCode == 302 || responseCode == 406) {
                        System.out.println(space + name + " :: " + urlString
                                + " :: OK");

                        outputWriter.println(space + "* [" + name + "]("
                                + urlString + ")  ");
                        outputWriter.flush();
                    } else {
                        if (responseCode > 0) {
                            System.out.println(name + " :: " + urlString
                                    + " :: " + responseCode);
                            errorWriter
                                    .println(space + "* [" + name + "]("
                                            + urlString + ") :: "
                                            + responseCode + "  ");
                            errorWriter.flush();
                        }
                    }
                }

                if (statusCheck == false) {
                    System.out.println(space + name + " :: " + urlString);

                    outputWriter.println(space + "* [" + name + "]("
                            + urlString + ")  ");
                    outputWriter.flush();
                }
            }
        }
        outputWriter.close();
        errorWriter.close();
    }

    public static void printHeader() {
        System.out.println("");
        System.out.println("Bookmark Extractor version 0.1");
        System.out.println("Selective bookmark extractor and formatter.");
        System.out.println("");
    }
}