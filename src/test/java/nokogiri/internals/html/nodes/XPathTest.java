package nokogiri.internals.html.nodes;

import org.junit.jupiter.api.Test;
import nokogiri.internals.html.TextUtil;
import nokogiri.internals.html.parser.Parser;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

public class XPathTest {
    @Test
    public void testXPath() {
        String html = "<html>\n" +
                "          <head></head>\n" +
                "          <body>\n" +
                "            <div class='baz'><a href=\"foo\" class=\"bar\">first</a></div>\n" +
                "          </body>\n" +
                "        </html>";
        org.w3c.dom.Document doc = Parser.parse(html, "");
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression expr = null;
        try {
            expr = xpath.compile("/html/body/div/a");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                System.out.println(nodes.item(i).getNodeValue());
            }
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testXPath2() {
        String html = "<html>\n" +
                "          <head></head>\n" +
                "          <body>\n" +
                "            <div class='baz'><a href=\"foo\" class=\"bar\">first</a></div>\n" +
                "          </body>\n" +
                "        </html>";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(html));
            org.w3c.dom.Document doc = builder.parse(is);
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            XPathExpression expr = xpath.compile("/html/body/div/a");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                System.out.println(nodes.item(i).getNodeValue());
            }
        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            throw new RuntimeException(e);
        }

    }
}
