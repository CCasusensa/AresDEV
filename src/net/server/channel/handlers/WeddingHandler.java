/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.server.channel.handlers;

import net.AbstractMaplePacketHandler;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;

/**
 *
 * @author Kevin
 */
public class WeddingHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// System.out.println("Wedding Packet: " + slea);
		final MapleCharacter chr = c.getPlayer();
		final byte operation = slea.readByte();
		switch (operation) {
		case 0x06:// Add an item to the Wedding Registry
			final byte slot = (byte) slea.readShort();
			final int itemid = slea.readInt();
			final short quantity = slea.readShort();
			final MapleInventoryType type = MapleItemInformationProvider
					.getInstance().getInventoryType(itemid);
			final Item item = chr.getInventory(type).getItem(slot);
			if ((itemid == item.getItemId())
					&& (quantity <= item.getQuantity())) {
				c.announce(MaplePacketCreator.addItemToWeddingRegistry(chr,
						item));
			}
		}
	}
}
