Bookmark Extractor
--------------------------------------------------------------------------------
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](./LICENSE.md)

Bookmark Extractor is a selective bookmark extractor and formatter.  
Chromium on Linux as an input source and Markdown as an output format are currently supported.  
Support for other operating systems, browsers and output formats may be added in the future.  
Bookmark Extractor is still in an early stage of development.  

## Dependency
* [Google Gson v.2.6.2](https://github.com/google/gson)  

## Compiling Using Maven
```
mvn package  
```

## Compiling Using Eclipse
``gson-2.6.2.jar`` has to be manually downloaded in the root directory of the project.  
This version of the Google Gson library could be obtained from [here](https://repo1.maven.org/maven2/com/google/code/gson/gson/2.6.2/gson-2.6.2.jar).

## Usage
```
Bookmark Extractor version 0.1  
Selective bookmark extractor and formatter.  

java -jar bookmark-extractor.jar --root=root_folder  
java -jar bookmark-extractor.jar --root="root folder with spaces"  

Arguments:  
--help                this help  
--root=<node-name>    root node - mandatory argument  
--no-check            do not check URLs  
```

## [Thanks and Credits](./CREDITS.md)

## [License](./LICENSE.md)
Bookmark Extractor is licensed under the GNU General Public License Version 3.  
Dimitar D. Mitov 2017  
