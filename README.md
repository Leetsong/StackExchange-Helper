# StackExchange-Helper
A StackExchange Helper (SEH, or seh for short) for academic researchers. 
Researchers can crawl questions with specific tags, and output them to
csv files.

### Build

#### 1. Without dependencies

```sh
mvn package
```

#### 2. With dependencies

```sh
mvn assembly:single
```

### Usage

#### In general

```sh
java -jar seh.jar <command> [options]
<command>:
  fetch    fetch interested queries using StackExchange API
  goo      fetch interested queries using Google Search
  combine  combine fetched csv results
  config   change seh configs
  version  show version
  help     show this
```