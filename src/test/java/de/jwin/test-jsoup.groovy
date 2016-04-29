package de.jwin

@Grab('org.jsoup:jsoup:1.9.1')

def doc = org.jsoup.Jsoup.connect("http://faz.net").get()

doc.select("*").each {
    node -> println "${node.text()}"
}