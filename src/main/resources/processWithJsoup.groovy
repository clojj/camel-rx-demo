@Grab('org.jsoup:jsoup:1.9.1')

def doc = org.jsoup.Jsoup.parse(request.body)

doc.select("*").each {
    println "-----------------------"
    println "<${it.tagName()}> id: ${it.id()}"
    println "${it.text()}"
}