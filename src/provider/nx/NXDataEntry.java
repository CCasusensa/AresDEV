package provider.nx;

import provider.MapleDataEntity;
import provider.MapleDataEntry;
import provider.nx.core.format.NXNode;

/**
 * @author Aaron
 * @version 1.0
 * @since 6/8/13
 */
public class NXDataEntry implements MapleDataEntry {
	private final NXNode node;
	private final NXData parent;

	public NXDataEntry(NXNode node, NXData parent) {
		this.node = node;
		this.parent = parent;
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
		return 20; // ALL NODES ARE 20-BYTES.
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
