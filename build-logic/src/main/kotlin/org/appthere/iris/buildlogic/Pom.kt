package org.appthere.iris.buildlogic

import org.w3c.dom.Element
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/** The parts of a POM the license check reads; [parent] is `group:artifact:version`. */
internal data class Pom(
    val licenses: List<PomLicense>,
    val parent: String?,
)

internal fun parsePom(xml: String): Pom {
    val factory = DocumentBuilderFactory.newInstance()
    // POMs come from the network: no DTDs, no external entities.
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    val root = factory.newDocumentBuilder().parse(xml.byteInputStream()).documentElement
    val licenses =
        root.child("licenses")?.children("license").orEmpty().map {
            PomLicense(it.child("name")?.textContent?.trim().orEmpty(), it.child("url")?.textContent?.trim().orEmpty())
        }
    val parent =
        root.child("parent")?.let { p ->
            listOf("groupId", "artifactId", "version").map { p.child(it)?.textContent?.trim().orEmpty() }.joinToString(":")
        }
    return Pom(licenses, parent)
}

private fun Element.children(name: String): List<Element> =
    (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>().filter { it.localNameOrTag() == name }

private fun Element.child(name: String): Element? = children(name).firstOrNull()

private fun Element.localNameOrTag(): String = localName ?: tagName
