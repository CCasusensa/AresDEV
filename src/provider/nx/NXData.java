package provider.nx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import provider.MapleData;
import provider.MapleDataEntity;
import provider.nx.core.format.NXNode;
import provider.nx.core.format.nodes.NXAudioNode;
import provider.nx.core.format.nodes.NXBitmapNode;
import provider.nx.core.format.nodes.NXDoubleNode;
import provider.nx.core.format.nodes.NXLongNode;
import provider.nx.core.format.nodes.NXNullNode;
import provider.nx.core.format.nodes.NXPointNode;
import provider.nx.core.format.nodes.NXStringNode;
import provider.wz.MapleDataType;

/**
 * @author Aaron
 * @version 1.0
 * @since 6/8/13
 */
public class NXData implements MapleData {
	private final NXNode node;
	private final NXData parent;

	public NXData(NXNode node, NXData parent) {
		this.node = node;
		this.parent = parent;
	}

	public NXNode getNode() {
		return this.node;
	}

	public NXData getParentAsNX() {
		return new NXData(this.parent.getNode(), this.parent.getParentAsNX());
	}

	@Override
	public String getName() {
		return this.node.getName();
	}

	@Override
	public MapleDataEntity getParent() {
		return new NXDataEntity(this.node, this.parent);
	}

	@Override
	public MapleDataType getType() {
		if (this.node instanceof NXBitmapNode) {
			return MapleDataType.CANVAS;
		} else if (this.node instanceof NXPointNode) {
			return MapleDataType.VECTOR;
		} else if (this.node instanceof NXNullNode) {
			return MapleDataType.NONE;
		} else if (this.node instanceof NXAudioNode) {
			return MapleDataType.SOUND;
		} else if (this.node instanceof NXLongNode) {
			return MapleDataType.INT;
		} else if (this.node instanceof NXDoubleNode) {
			return MapleDataType.DOUBLE;
		} else if (this.node instanceof NXStringNode) {
			return MapleDataType.STRING;
		} else {
			return MapleDataType.UNKNOWN_TYPE;
		}
	}

	@Override
	public List<MapleData> getChildren() {
		final List<MapleData> md = new ArrayList<MapleData>();
		for (final NXNode child : this.node) {
			md.add(new NXData(child, new NXData(this.node, this.parent)));
		}
		return md;
	}

	@Override
	public MapleData getChildByPath(String path) {
		NXNode cursor = this.node;
		NXData parent = null;
		for (final String child : path.split("/")) {
			if (cursor == null) {
				return null;
			}
			parent = new NXData(cursor, parent);
			cursor = cursor.getChild(child);
		}
		return new NXData(cursor, parent);
	}

	@Override
	public Object getData() {
		return this.node.get();
	}

	@Override
	public Iterator<MapleData> iterator() {
		return new Iterator<MapleData>() {
			@Override
			public boolean hasNext() {
				return NXData.this.node.iterator().hasNext();
			}

			@Override
			public MapleData next() {
				return new NXData(NXData.this.node.iterator().next(),
						new NXData(NXData.this.node, NXData.this.parent));
			}

			@Override
			public void remove() {
				NXData.this.node.iterator().remove();
			}
		};
	}
}
