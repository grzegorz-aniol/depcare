package com.appga.depcare.utils

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.util.stream.Stream
import kotlin.streams.asSequence

fun Element.getFirstElementValue(name: String): String? {
    val element = firstChildElement(name)
    return element?.textContent
}

fun Element.getFirstElement(name: String): Element? {
    return firstChildElement(name)
}

fun NodeList.forEach(action: (Element) -> Unit) {
    for (i in 0..length) {
        val node = item(i)
        if (node is Element) {
            action(node)
        }
    }
}

fun NodeList.asSequence(): Sequence<Element> {
    return Stream.iterate(0, { it < length }, { it + 1 })
        .asSequence()
        .map { item(it) }
        .filterIsInstance(Element::class.java)
}

fun Element.firstChildElement(name: String): Element? {
    for (i in 0..childNodes.length) {
        val node = childNodes.item(i)
        if (node is Element) {
            if (node.tagName == name) {
                return node
            }
        }
    }
    return null
}
