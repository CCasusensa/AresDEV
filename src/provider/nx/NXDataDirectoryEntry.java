package provider.nx;

import java.util.ArrayList;
import java.util.List;

import provider.MapleDataDirectoryEntry;
import provider.MapleDataEntity;
import provider.MapleDataEntry;
import provider.MapleDataFileEntry;
import provider.nx.core.format.NXNode;
import provider.nx.core.format.nodes.NXNullNode;

/**
 * @author Aaron
 * @version 1.0
 * @since 6/8/13
 */
public class NXDataDirectoryEntry implements MapleDataDirectoryEntry {
	private final NXNode node;
	private final NXData parent;

	public NXDataDirectoryEntry(NXNode node, NXData parent) {
		this.node = node;
		this.parent = parent;
	}

	@Override
	public List<MapleDataDirectoryEntry> getSubdirectories() {
		final List<MapleDataDirectoryEntry> mdde = new ArrayList<>();
		for (final NXNode child : this.node) {
			if (child instanceof NXNullNode) {
				mdde.add(new NXDataDirectoryEntry(child, new NXData(this.node,
						this.parent)));
			}
		}
		return mdde;
	}

	@Override
	public List<MapleDataFileEntry> getFiles() {
		final List<MapleDataFileEntry> mdde = new ArrayList<>();
		for (final NXNode child : this.node) {
			if (!(child instanceof NXNullNode)) {
				mdde.add(new NXDataFileEntry(child, new NXData(this.node,
						this.parent)));
			}
		}
		return mdde;
	}

	@Override
	public MapleDataEntry getEntry(String name) {
		return new NXDataEntry(this.node.getChild(name), new NXData(this.node,
				this.parent));
	}

	@Override
	public String getName() {
		return this.node.getName();
	}

	@Override
	public MapleDataEntity getParent() {
		return new NXDataEntity(this.parent.getNode(),
				this.parent.getParentAsNX());
	}

	@Override
	public int getSize() {
		return this.node.getChildCount();
	}

	@Override
	public int getChecksum() {
		return -1; // NOT USED ANYWHERE IN THE SOURCE.
	}

	@Override
	public int getOffset() {
		int i = 0;
		final NXNode[] nodes = this.node.getFile().getNodes();
		for (; i < nodes.length; i++) {
			if (nodes[i] == this.node) {
				break;
			}
		}
		return (int) (this.node.getFile().getHeader().getNodeOffset() + (20 * i));
	}
}
