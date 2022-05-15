package com.appga.depcare.supplier.utils

import org.w3c.dom.CharacterData
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

fun Element.forEach(action: (Element) -> Unit) {
    var child = firstChild
    while (child != null) {
        if (child is Element) {
            action(child)
        }
        child = child.nextSibling
    }
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

fun Element.getTextValue(): String? {
    firstChild?.let {
        if (it is CharacterData) {
            return it.data
        }
    }
    return null
}
