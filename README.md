Bookmark Extractor
--------------------------------------------------------------------------------
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](./LICENSE.md)

Bookmark Extractor is my personal selective bookmark extractor and formatter.  
Bookmarks of Chromium for Linux and Chrome for Windows are supported as input source.  
Markdown is the only supported output format.  

## Dependency
* [Google Gson](https://github.com/google/gson) v.2.6.2  

## Compiling Using Maven
```
mvn package  
```

## Compiling Using Eclipse
``gson-2.6.2.jar`` has to be manually downloaded in the root directory of the project.  
This version of the Google Gson library could be obtained from [here](https://repo1.maven.org/maven2/com/google/code/gson/gson/2.6.2/gson-2.6.2.jar).

## Usage
```
Bookmark Extractor version 0.2.5  
Selective bookmark extractor and formatter.  

java -jar bookmark-extractor.jar --root=root_folder  
java -jar bookmark-extractor.jar --root="root folder with spaces"  

Arguments:  
--help                this help  
--root=<node-name>    root node - mandatory argument  
--no-check            do not check URLs  
```

## [Credits](./CREDITS.md)

## [License](./LICENSE.md)
Bookmark Extractor is licensed under the GNU General Public License Version 3.  
Dimitar D. Mitov 2017 - 2020  
