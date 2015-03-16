/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package provider.wz;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import provider.MapleData;
import provider.MapleDataEntity;

public class XMLDomMapleData implements MapleData {
	private Node node;
	private File imageDataDir;

	public XMLDomMapleData(FileInputStream fis, File imageDataDir) {
		try {
			final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
					.newInstance();
			final DocumentBuilder documentBuilder = documentBuilderFactory
					.newDocumentBuilder();
			final Document document = documentBuilder.parse(fis);
			this.node = document.getFirstChild();
		} catch (final ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (final SAXException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		this.imageDataDir = imageDataDir;
	}

	private XMLDomMapleData(Node node) {
		this.node = node;
	}

	@Override
	public MapleData getChildByPath(String path) {
		final String segments[] = path.split("/");
		if (segments[0].equals("..")) {
			return ((MapleData) this.getParent()).getChildByPath(path
					.substring(path.indexOf("/") + 1));
		}

		Node myNode = this.node;
		for (final String segment : segments) {
			final NodeList childNodes = myNode.getChildNodes();
			boolean foundChild = false;
			for (int i = 0; i < childNodes.getLength(); i++) {
				final Node childNode = childNodes.item(i);
				if ((childNode.getNodeType() == Node.ELEMENT_NODE)
						&& childNode.getAttributes().getNamedItem("name")
								.getNodeValue().equals(segment)) {
					myNode = childNode;
					foundChild = true;
					break;
				}
			}
			if (!foundChild) {
				return null;
			}
		}
		final XMLDomMapleData ret = new XMLDomMapleData(myNode);
		ret.imageDataDir = new File(this.imageDataDir, this.getName() + "/"
				+ path).getParentFile();
		return ret;
	}

	@Override
	public List<MapleData> getChildren() {
		final List<MapleData> ret = new ArrayList<MapleData>();
		final NodeList childNodes = this.node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			final Node childNode = childNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				final XMLDomMapleData child = new XMLDomMapleData(childNode);
				child.imageDataDir = new File(this.imageDataDir, this.getName());
				ret.add(child);
			}
		}
		return ret;
	}

	@Override
	public Object getData() {
		final NamedNodeMap attributes = this.node.getAttributes();
		final MapleDataType type = this.getType();
		switch (type) {
		case DOUBLE:
		case FLOAT:
		case INT:
		case SHORT:
		case STRING:
		case UOL: {
			final String value = attributes.getNamedItem("value")
					.getNodeValue();
			switch (type) {
			case DOUBLE:
				return Double.valueOf(Double.parseDouble(value));
			case FLOAT:
				return Float.valueOf(Float.parseFloat(value));
			case INT:
				return Integer.valueOf(Integer.parseInt(value));
			case SHORT:
				return Short.valueOf(Short.parseShort(value));
			case STRING:
			case UOL:
				return value;
			}
		}
		case VECTOR: {
			final String x = attributes.getNamedItem("x").getNodeValue();
			final String y = attributes.getNamedItem("y").getNodeValue();
			return new Point(Integer.parseInt(x), Integer.parseInt(y));
		}
		case CANVAS: {
			final String width = attributes.getNamedItem("width")
					.getNodeValue();
			final String height = attributes.getNamedItem("height")
					.getNodeValue();
			return new FileStoredPngMapleCanvas(Integer.parseInt(width),
					Integer.parseInt(height), new File(this.imageDataDir,
							this.getName() + ".png"));
		}
		}
		return null;
	}

	@Override
	public MapleDataType getType() {
		final String nodeName = this.node.getNodeName();
		if (nodeName.equals("imgdir")) {
			return MapleDataType.PROPERTY;
		} else if (nodeName.equals("canvas")) {
			return MapleDataType.CANVAS;
		} else if (nodeName.equals("convex")) {
			return MapleDataType.CONVEX;
		} else if (nodeName.equals("sound")) {
			return MapleDataType.SOUND;
		} else if (nodeName.equals("uol")) {
			return MapleDataType.UOL;
		} else if (nodeName.equals("double")) {
			return MapleDataType.DOUBLE;
		} else if (nodeName.equals("float")) {
			return MapleDataType.FLOAT;
		} else if (nodeName.equals("int")) {
			return MapleDataType.INT;
		} else if (nodeName.equals("short")) {
			return MapleDataType.SHORT;
		} else if (nodeName.equals("string")) {
			return MapleDataType.STRING;
		} else if (nodeName.equals("vector")) {
			return MapleDataType.VECTOR;
		} else if (nodeName.equals("null")) {
			return MapleDataType.IMG_0x00;
		}
		return null;
	}

	@Override
	public MapleDataEntity getParent() {
		final Node parentNode = this.node.getParentNode();
		if (parentNode.getNodeType() == Node.DOCUMENT_NODE) {
			return null;
		}
		final XMLDomMapleData parentData = new XMLDomMapleData(parentNode);
		parentData.imageDataDir = this.imageDataDir.getParentFile();
		return parentData;
	}

	@Override
	public String getName() {
		return this.node.getAttributes().getNamedItem("name").getNodeValue();
	}

	@Override
	public Iterator<MapleData> iterator() {
		return this.getChildren().iterator();
	}
}
