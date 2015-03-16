package client.inventory;

import constants.ItemConstants;

/**
 *
 * @author kevin
 */
public class ModifyInventory {

	private final int mode;
	private Item item;
	private short oldPos;

	public ModifyInventory(final int mode, final Item item) {
		this.mode = mode;
		this.item = item.copy();
	}

	public ModifyInventory(final int mode, final Item item, final short oldPos) {
		this.mode = mode;
		this.item = item.copy();
		this.oldPos = oldPos;
	}

	public final int getMode() {
		return this.mode;
	}

	public final int getInventoryType() {
		return ItemConstants.getInventoryType(this.item.getItemId()).getType();
	}

	public final short getPosition() {
		return this.item.getPosition();
	}

	public final short getOldPosition() {
		return this.oldPos;
	}

	public final short getQuantity() {
		return this.item.getQuantity();
	}

	public final Item getItem() {
		return this.item;
	}

	public final void clear() {
		this.item = null;
	}
}