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
package server;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.DatabaseConnection;
import tools.Pair;
import tools.Randomizer;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import constants.ItemConstants;

/**
 *
 * @author Matze
 *
 */
public class MapleItemInformationProvider {

	private static MapleItemInformationProvider instance = null;
	protected MapleDataProvider itemData;
	protected MapleDataProvider equipData;
	protected MapleDataProvider stringData;
	protected MapleData cashStringData;
	protected MapleData consumeStringData;
	protected MapleData eqpStringData;
	protected MapleData etcStringData;
	protected MapleData insStringData;
	protected MapleData petStringData;
	protected Map<Integer, MapleInventoryType> inventoryTypeCache = new HashMap<>();
	protected Map<Integer, Short> slotMaxCache = new HashMap<>();
	protected Map<Integer, MapleStatEffect> itemEffects = new HashMap<>();
	protected Map<Integer, Map<String, Integer>> equipStatsCache = new HashMap<>();
	protected Map<Integer, Equip> equipCache = new HashMap<>();
	protected Map<Integer, Double> priceCache = new HashMap<>();
	protected Map<Integer, Integer> wholePriceCache = new HashMap<>();
	protected Map<Integer, Integer> projectileWatkCache = new HashMap<>();
	protected Map<Integer, String> nameCache = new HashMap<>();
	protected Map<Integer, String> descCache = new HashMap<>();
	protected Map<Integer, String> msgCache = new HashMap<>();
	protected Map<Integer, Boolean> dropRestrictionCache = new HashMap<>();
	protected Map<Integer, Boolean> pickupRestrictionCache = new HashMap<>();
	protected Map<Integer, Integer> getMesoCache = new HashMap<>();
	protected Map<Integer, Integer> monsterBookID = new HashMap<>();
	protected Map<Integer, Boolean> onEquipUntradableCache = new HashMap<>();
	protected Map<Integer, scriptedItem> scriptedItemCache = new HashMap<>();
	protected Map<Integer, Boolean> karmaCache = new HashMap<>();
	protected Map<Integer, Integer> triggerItemCache = new HashMap<>();
	protected Map<Integer, Integer> expCache = new HashMap<>();
	protected Map<Integer, Integer> levelCache = new HashMap<>();
	protected Map<Integer, Pair<Integer, List<RewardItem>>> rewardCache = new HashMap<>();
	protected List<Pair<Integer, String>> itemNameCache = new ArrayList<>();
	protected Map<Integer, Boolean> consumeOnPickupCache = new HashMap<>();
	protected Map<Integer, Boolean> isQuestItemCache = new HashMap<>();

	private MapleItemInformationProvider() {
		this.loadCardIdData();
		this.itemData = MapleDataProviderFactory.getDataProvider(new File(
				System.getProperty("wzpath") + "/Item.wz"));
		this.equipData = MapleDataProviderFactory.getDataProvider(new File(
				System.getProperty("wzpath") + "/Character.wz"));
		this.stringData = MapleDataProviderFactory.getDataProvider(new File(
				System.getProperty("wzpath") + "/String.wz"));
		this.cashStringData = this.stringData.getData("Cash.img");
		this.consumeStringData = this.stringData.getData("Consume.img");
		this.eqpStringData = this.stringData.getData("Eqp.img");
		this.etcStringData = this.stringData.getData("Etc.img");
		this.insStringData = this.stringData.getData("Ins.img");
		this.petStringData = this.stringData.getData("Pet.img");
	}

	public static MapleItemInformationProvider getInstance() {
		if (instance == null) {
			instance = new MapleItemInformationProvider();
		}
		return instance;
	}

	public MapleInventoryType getInventoryType(int itemId) {
		if (this.inventoryTypeCache.containsKey(itemId)) {
			return this.inventoryTypeCache.get(itemId);
		}
		MapleInventoryType ret;
		final String idStr = "0" + String.valueOf(itemId);
		MapleDataDirectoryEntry root = this.itemData.getRoot();
		for (final MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
			for (final MapleDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
					ret = MapleInventoryType.getByWZName(topDir.getName());
					this.inventoryTypeCache.put(itemId, ret);
					return ret;
				} else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
					ret = MapleInventoryType.getByWZName(topDir.getName());
					this.inventoryTypeCache.put(itemId, ret);
					return ret;
				}
			}
		}
		root = this.equipData.getRoot();
		for (final MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
			for (final MapleDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr + ".img")) {
					ret = MapleInventoryType.EQUIP;
					this.inventoryTypeCache.put(itemId, ret);
					return ret;
				}
			}
		}
		ret = MapleInventoryType.UNDEFINED;
		this.inventoryTypeCache.put(itemId, ret);
		return ret;
	}

	public List<Pair<Integer, String>> getAllItems() {
		if (!this.itemNameCache.isEmpty()) {
			return this.itemNameCache;
		}
		final List<Pair<Integer, String>> itemPairs = new ArrayList<>();
		MapleData itemsData;
		itemsData = this.stringData.getData("Cash.img");
		for (final MapleData itemFolder : itemsData.getChildren()) {
			itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()),
					MapleDataTool.getString("name", itemFolder, "NO-NAME")));
		}
		itemsData = this.stringData.getData("Consume.img");
		for (final MapleData itemFolder : itemsData.getChildren()) {
			itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()),
					MapleDataTool.getString("name", itemFolder, "NO-NAME")));
		}
		itemsData = this.stringData.getData("Eqp.img").getChildByPath("Eqp");
		for (final MapleData eqpType : itemsData.getChildren()) {
			for (final MapleData itemFolder : eqpType.getChildren()) {
				itemPairs.add(new Pair<>(
						Integer.parseInt(itemFolder.getName()), MapleDataTool
								.getString("name", itemFolder, "NO-NAME")));
			}
		}
		itemsData = this.stringData.getData("Etc.img").getChildByPath("Etc");
		for (final MapleData itemFolder : itemsData.getChildren()) {
			itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()),
					MapleDataTool.getString("name", itemFolder, "NO-NAME")));
		}
		itemsData = this.stringData.getData("Ins.img");
		for (final MapleData itemFolder : itemsData.getChildren()) {
			itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()),
					MapleDataTool.getString("name", itemFolder, "NO-NAME")));
		}
		itemsData = this.stringData.getData("Pet.img");
		for (final MapleData itemFolder : itemsData.getChildren()) {
			itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()),
					MapleDataTool.getString("name", itemFolder, "NO-NAME")));
		}
		return itemPairs;
	}

	private MapleData getStringData(int itemId) {
		String cat = "null";
		MapleData theData;
		if (itemId >= 5010000) {
			theData = this.cashStringData;
		} else if ((itemId >= 2000000) && (itemId < 3000000)) {
			theData = this.consumeStringData;
		} else if (((itemId >= 1010000) && (itemId < 1040000))
				|| ((itemId >= 1122000) && (itemId < 1123000))
				|| ((itemId >= 1142000) && (itemId < 1143000))) {
			theData = this.eqpStringData;
			cat = "Eqp/Accessory";
		} else if ((itemId >= 1000000) && (itemId < 1010000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Cap";
		} else if ((itemId >= 1102000) && (itemId < 1103000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Cape";
		} else if ((itemId >= 1040000) && (itemId < 1050000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Coat";
		} else if ((itemId >= 20000) && (itemId < 22000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Face";
		} else if ((itemId >= 1080000) && (itemId < 1090000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Glove";
		} else if ((itemId >= 30000) && (itemId < 32000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Hair";
		} else if ((itemId >= 1050000) && (itemId < 1060000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Longcoat";
		} else if ((itemId >= 1060000) && (itemId < 1070000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Pants";
		} else if ((itemId >= 1802000) && (itemId < 1810000)) {
			theData = this.eqpStringData;
			cat = "Eqp/PetEquip";
		} else if ((itemId >= 1112000) && (itemId < 1120000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Ring";
		} else if ((itemId >= 1092000) && (itemId < 1100000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Shield";
		} else if ((itemId >= 1070000) && (itemId < 1080000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Shoes";
		} else if ((itemId >= 1900000) && (itemId < 2000000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Taming";
		} else if ((itemId >= 1300000) && (itemId < 1800000)) {
			theData = this.eqpStringData;
			cat = "Eqp/Weapon";
		} else if ((itemId >= 4000000) && (itemId < 5000000)) {
			theData = this.etcStringData;
		} else if ((itemId >= 3000000) && (itemId < 4000000)) {
			theData = this.insStringData;
		} else if ((itemId >= 5000000) && (itemId < 5010000)) {
			theData = this.petStringData;
		} else {
			return null;
		}
		if (cat.equalsIgnoreCase("null")) {
			return theData.getChildByPath(String.valueOf(itemId));
		} else {
			return theData.getChildByPath(cat + "/" + itemId);
		}
	}

	public boolean noCancelMouse(int itemId) {
		final MapleData item = this.getItemData(itemId);
		if (item == null) {
			return false;
		}
		return MapleDataTool.getIntConvert("info/noCancelMouse", item, 0) == 1;
	}

	private MapleData getItemData(int itemId) {
		MapleData ret = null;
		final String idStr = "0" + String.valueOf(itemId);
		MapleDataDirectoryEntry root = this.itemData.getRoot();
		for (final MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
			for (final MapleDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
					ret = this.itemData.getData(topDir.getName() + "/"
							+ iFile.getName());
					if (ret == null) {
						return null;
					}
					ret = ret.getChildByPath(idStr);
					return ret;
				} else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
					return this.itemData.getData(topDir.getName() + "/"
							+ iFile.getName());
				}
			}
		}
		root = this.equipData.getRoot();
		for (final MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
			for (final MapleDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr + ".img")) {
					return this.equipData.getData(topDir.getName() + "/"
							+ iFile.getName());
				}
			}
		}
		return ret;
	}

	public short getSlotMax(MapleClient c, int itemId) {
		if (this.slotMaxCache.containsKey(itemId)) {
			return this.slotMaxCache.get(itemId);
		}
		short ret = 0;
		final MapleData item = this.getItemData(itemId);
		if (item != null) {
			final MapleData smEntry = item.getChildByPath("info/slotMax");
			if (smEntry == null) {
				if (this.getInventoryType(itemId).getType() == MapleInventoryType.EQUIP
						.getType()) {
					ret = 1;
				} else {
					ret = 100;
				}
			} else {
				ret = (short) MapleDataTool.getInt(smEntry);
				if (ItemConstants.isThrowingStar(itemId)) {
					ret += c.getPlayer().getSkillLevel(
							SkillFactory.getSkill(4100000)) * 10;
				} else {
					ret += c.getPlayer().getSkillLevel(
							SkillFactory.getSkill(5200000)) * 10;
				}
			}
		}
		if (!ItemConstants.isRechargable(itemId)) {
			this.slotMaxCache.put(itemId, ret);
		}
		return ret;
	}

	public int getMeso(int itemId) {
		if (this.getMesoCache.containsKey(itemId)) {
			return this.getMesoCache.get(itemId);
		}
		final MapleData item = this.getItemData(itemId);
		if (item == null) {
			return -1;
		}
		int pEntry;
		final MapleData pData = item.getChildByPath("info/meso");
		if (pData == null) {
			return -1;
		}
		pEntry = MapleDataTool.getInt(pData);
		this.getMesoCache.put(itemId, pEntry);
		return pEntry;
	}

	public int getWholePrice(int itemId) {
		if (this.wholePriceCache.containsKey(itemId)) {
			return this.wholePriceCache.get(itemId);
		}
		final MapleData item = this.getItemData(itemId);
		if (item == null) {
			return -1;
		}
		int pEntry;
		final MapleData pData = item.getChildByPath("info/price");
		if (pData == null) {
			return -1;
		}
		pEntry = MapleDataTool.getInt(pData);
		this.wholePriceCache.put(itemId, pEntry);
		return pEntry;
	}

	public double getPrice(int itemId) {
		if (this.priceCache.containsKey(itemId)) {
			return this.priceCache.get(itemId);
		}
		final MapleData item = this.getItemData(itemId);
		if (item == null) {
			return -1;
		}
		double pEntry;
		MapleData pData = item.getChildByPath("info/unitPrice");
		if (pData != null) {
			try {
				pEntry = MapleDataTool.getDouble(pData);
			} catch (final Exception e) {
				pEntry = MapleDataTool.getInt(pData);
			}
		} else {
			pData = item.getChildByPath("info/price");
			if (pData == null) {
				return -1;
			}
			pEntry = MapleDataTool.getInt(pData);
		}
		this.priceCache.put(itemId, pEntry);
		return pEntry;
	}

	protected Map<String, Integer> getEquipStats(int itemId) {
		if (this.equipStatsCache.containsKey(itemId)) {
			return this.equipStatsCache.get(itemId);
		}
		final Map<String, Integer> ret = new LinkedHashMap<>();
		final MapleData item = this.getItemData(itemId);
		if (item == null) {
			return null;
		}
		final MapleData info = item.getChildByPath("info");
		if (info == null) {
			return null;
		}
		for (final MapleData data : info.getChildren()) {
			if (data.getName().startsWith("inc")) {
				ret.put(data.getName().substring(3),
						MapleDataTool.getIntConvert(data));
			}
			/*
			 * else if (data.getName().startsWith("req"))
			 * ret.put(data.getName(), MapleDataTool.getInt(data.getName(),
			 * info, 0));
			 */
		}
		ret.put("reqJob", MapleDataTool.getInt("reqJob", info, 0));
		ret.put("reqLevel", MapleDataTool.getInt("reqLevel", info, 0));
		ret.put("reqDEX", MapleDataTool.getInt("reqDEX", info, 0));
		ret.put("reqSTR", MapleDataTool.getInt("reqSTR", info, 0));
		ret.put("reqINT", MapleDataTool.getInt("reqINT", info, 0));
		ret.put("reqLUK", MapleDataTool.getInt("reqLUK", info, 0));
		ret.put("reqPOP", MapleDataTool.getInt("reqPOP", info, 0));
		ret.put("cash", MapleDataTool.getInt("cash", info, 0));
		ret.put("tuc", MapleDataTool.getInt("tuc", info, 0));
		ret.put("cursed", MapleDataTool.getInt("cursed", info, 0));
		ret.put("success", MapleDataTool.getInt("success", info, 0));
		ret.put("fs", MapleDataTool.getInt("fs", info, 0));
		this.equipStatsCache.put(itemId, ret);
		return ret;
	}

	public List<Integer> getScrollReqs(int itemId) {
		final List<Integer> ret = new ArrayList<>();
		MapleData data = this.getItemData(itemId);
		data = data.getChildByPath("req");
		if (data == null) {
			return ret;
		}
		for (final MapleData req : data.getChildren()) {
			ret.add(MapleDataTool.getInt(req));
		}
		return ret;
	}

	public MapleWeaponType getWeaponType(int itemId) {
		final int cat = (itemId / 10000) % 100;
		final MapleWeaponType[] type = { MapleWeaponType.SWORD1H,
				MapleWeaponType.AXE1H, MapleWeaponType.BLUNT1H,
				MapleWeaponType.DAGGER, MapleWeaponType.NOT_A_WEAPON,
				MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.NOT_A_WEAPON,
				MapleWeaponType.WAND, MapleWeaponType.STAFF,
				MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.SWORD2H,
				MapleWeaponType.AXE2H, MapleWeaponType.BLUNT2H,
				MapleWeaponType.SPEAR, MapleWeaponType.POLE_ARM,
				MapleWeaponType.BOW, MapleWeaponType.CROSSBOW,
				MapleWeaponType.CLAW, MapleWeaponType.KNUCKLE,
				MapleWeaponType.GUN };
		if ((cat < 30) || (cat > 49)) {
			return MapleWeaponType.NOT_A_WEAPON;
		}
		return type[cat - 30];
	}

	private boolean isCleanSlate(int scrollId) {
		return (scrollId > 2048999) && (scrollId < 2049004);
	}

	public Item scrollEquipWithId(Item equip, int scrollId,
			boolean usingWhiteScroll, boolean isGM) {
		if (equip instanceof Equip) {
			final Equip nEquip = (Equip) equip;
			final Map<String, Integer> stats = this.getEquipStats(scrollId);
			final Map<String, Integer> eqstats = this.getEquipStats(equip
					.getItemId());
			if ((((nEquip.getUpgradeSlots() > 0) || this.isCleanSlate(scrollId)) && (Math
					.ceil(Math.random() * 100.0) <= stats.get("success")))
					|| isGM) {
				short flag = nEquip.getFlag();
				switch (scrollId) {
				case 2040727:
					flag |= ItemConstants.SPIKES;
					nEquip.setFlag((byte) flag);
					return equip;
				case 2041058:
					flag |= ItemConstants.COLD;
					nEquip.setFlag((byte) flag);
					return equip;
				case 2049000:
				case 2049001:
				case 2049002:
				case 2049003:
					if ((nEquip.getLevel() + nEquip.getUpgradeSlots()) < eqstats
							.get("tuc")) {
						nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + 1));
					}
					break;
				case 2049100:
				case 2049101:
				case 2049102:
					int inc = 1;
					if (Randomizer.nextInt(2) == 0) {
						inc = -1;
					}
					if (nEquip.getStr() > 0) {
						nEquip.setStr((short) Math.max(
								0,
								(nEquip.getStr() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getDex() > 0) {
						nEquip.setDex((short) Math.max(
								0,
								(nEquip.getDex() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getInt() > 0) {
						nEquip.setInt((short) Math.max(
								0,
								(nEquip.getInt() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getLuk() > 0) {
						nEquip.setLuk((short) Math.max(
								0,
								(nEquip.getLuk() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getWatk() > 0) {
						nEquip.setWatk((short) Math.max(
								0,
								(nEquip.getWatk() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getWdef() > 0) {
						nEquip.setWdef((short) Math.max(
								0,
								(nEquip.getWdef() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getMatk() > 0) {
						nEquip.setMatk((short) Math.max(
								0,
								(nEquip.getMatk() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getMdef() > 0) {
						nEquip.setMdef((short) Math.max(
								0,
								(nEquip.getMdef() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getAcc() > 0) {
						nEquip.setAcc((short) Math.max(
								0,
								(nEquip.getAcc() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getAvoid() > 0) {
						nEquip.setAvoid((short) Math.max(
								0,
								(nEquip.getAvoid() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getSpeed() > 0) {
						nEquip.setSpeed((short) Math.max(
								0,
								(nEquip.getSpeed() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getJump() > 0) {
						nEquip.setJump((short) Math.max(
								0,
								(nEquip.getJump() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getHp() > 0) {
						nEquip.setHp((short) Math.max(
								0,
								(nEquip.getHp() + (Randomizer.nextInt(6) * inc))));
					}
					if (nEquip.getMp() > 0) {
						nEquip.setMp((short) Math.max(
								0,
								(nEquip.getMp() + (Randomizer.nextInt(6) * inc))));
					}
					break;
				default:
					for (final Entry<String, Integer> stat : stats.entrySet()) {
						switch (stat.getKey()) {
						case "STR":
							nEquip.setStr((short) (nEquip.getStr() + stat
									.getValue().intValue()));
							break;
						case "DEX":
							nEquip.setDex((short) (nEquip.getDex() + stat
									.getValue().intValue()));
							break;
						case "INT":
							nEquip.setInt((short) (nEquip.getInt() + stat
									.getValue().intValue()));
							break;
						case "LUK":
							nEquip.setLuk((short) (nEquip.getLuk() + stat
									.getValue().intValue()));
							break;
						case "PAD":
							nEquip.setWatk((short) (nEquip.getWatk() + stat
									.getValue().intValue()));
							break;
						case "PDD":
							nEquip.setWdef((short) (nEquip.getWdef() + stat
									.getValue().intValue()));
							break;
						case "MAD":
							nEquip.setMatk((short) (nEquip.getMatk() + stat
									.getValue().intValue()));
							break;
						case "MDD":
							nEquip.setMdef((short) (nEquip.getMdef() + stat
									.getValue().intValue()));
							break;
						case "ACC":
							nEquip.setAcc((short) (nEquip.getAcc() + stat
									.getValue().intValue()));
							break;
						case "EVA":
							nEquip.setAvoid((short) (nEquip.getAvoid() + stat
									.getValue().intValue()));
							break;
						case "Speed":
							nEquip.setSpeed((short) (nEquip.getSpeed() + stat
									.getValue().intValue()));
							break;
						case "Jump":
							nEquip.setJump((short) (nEquip.getJump() + stat
									.getValue().intValue()));
							break;
						case "MHP":
							nEquip.setHp((short) (nEquip.getHp() + stat
									.getValue().intValue()));
							break;
						case "MMP":
							nEquip.setMp((short) (nEquip.getMp() + stat
									.getValue().intValue()));
							break;
						case "afterImage":
							break;
						}
					}
					break;
				}
				if (!this.isCleanSlate(scrollId)) {
					if (!isGM) {
						nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
					}
					nEquip.setLevel((byte) (nEquip.getLevel() + 1));
				}
			} else {
				if (!usingWhiteScroll && !this.isCleanSlate(scrollId) && !isGM) {
					nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
				}
				if (Randomizer.nextInt(101) < stats.get("cursed")) {
					return null;
				}
			}
		}
		return equip;
	}

	public Item getEquipById(int equipId) {
		return this.getEquipById(equipId, -1);
	}

	Item getEquipById(int equipId, int ringId) {
		Equip nEquip;
		nEquip = new Equip(equipId, (byte) 0, ringId);
		nEquip.setQuantity((short) 1);
		final Map<String, Integer> stats = this.getEquipStats(equipId);
		if (stats != null) {
			for (final Entry<String, Integer> stat : stats.entrySet()) {
				if (stat.getKey().equals("STR")) {
					nEquip.setStr((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("DEX")) {
					nEquip.setDex((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("INT")) {
					nEquip.setInt((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("LUK")) {
					nEquip.setLuk((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("PAD")) {
					nEquip.setWatk((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("PDD")) {
					nEquip.setWdef((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("MAD")) {
					nEquip.setMatk((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("MDD")) {
					nEquip.setMdef((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("ACC")) {
					nEquip.setAcc((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("EVA")) {
					nEquip.setAvoid((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("Speed")) {
					nEquip.setSpeed((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("Jump")) {
					nEquip.setJump((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("MHP")) {
					nEquip.setHp((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("MMP")) {
					nEquip.setMp((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("tuc")) {
					nEquip.setUpgradeSlots((byte) stat.getValue().intValue());
				} else if (this.isDropRestricted(equipId)) {
					byte flag = nEquip.getFlag();
					flag |= ItemConstants.UNTRADEABLE;
					nEquip.setFlag(flag);
				} else if (stats.get("fs") > 0) {
					byte flag = nEquip.getFlag();
					flag |= ItemConstants.SPIKES;
					nEquip.setFlag(flag);
					this.equipCache.put(equipId, nEquip);
				}
			}
		}
		return nEquip.copy();
	}

	private static short getRandStat(short defaultValue, int maxRange) {
		if (defaultValue == 0) {
			return 0;
		}
		final int lMaxRange = (int) Math.min(Math.ceil(defaultValue * 0.1),
				maxRange);
		return (short) ((defaultValue - lMaxRange) + Math.floor(Randomizer
				.nextDouble() * ((lMaxRange * 2) + 1)));
	}

	public Equip randomizeStats(Equip equip) {
		equip.setStr(getRandStat(equip.getStr(), 5));
		equip.setDex(getRandStat(equip.getDex(), 5));
		equip.setInt(getRandStat(equip.getInt(), 5));
		equip.setLuk(getRandStat(equip.getLuk(), 5));
		equip.setMatk(getRandStat(equip.getMatk(), 5));
		equip.setWatk(getRandStat(equip.getWatk(), 5));
		equip.setAcc(getRandStat(equip.getAcc(), 5));
		equip.setAvoid(getRandStat(equip.getAvoid(), 5));
		equip.setJump(getRandStat(equip.getJump(), 5));
		equip.setSpeed(getRandStat(equip.getSpeed(), 5));
		equip.setWdef(getRandStat(equip.getWdef(), 10));
		equip.setMdef(getRandStat(equip.getMdef(), 10));
		equip.setHp(getRandStat(equip.getHp(), 10));
		equip.setMp(getRandStat(equip.getMp(), 10));
		return equip;
	}

	public MapleStatEffect getItemEffect(int itemId) {
		MapleStatEffect ret = this.itemEffects.get(Integer.valueOf(itemId));
		if (ret == null) {
			final MapleData item = this.getItemData(itemId);
			if (item == null) {
				return null;
			}
			final MapleData spec = item.getChildByPath("spec");
			ret = MapleStatEffect.loadItemEffectFromData(spec, itemId);
			this.itemEffects.put(Integer.valueOf(itemId), ret);
		}
		return ret;
	}

	public int[][] getSummonMobs(int itemId) {
		final MapleData data = this.getItemData(itemId);
		final int theInt = data.getChildByPath("mob").getChildren().size();
		final int[][] mobs2spawn = new int[theInt][2];
		for (int x = 0; x < theInt; x++) {
			mobs2spawn[x][0] = MapleDataTool.getIntConvert("mob/" + x + "/id",
					data);
			mobs2spawn[x][1] = MapleDataTool.getIntConvert(
					"mob/" + x + "/prob", data);
		}
		return mobs2spawn;
	}

	public int getWatkForProjectile(int itemId) {
		Integer atk = this.projectileWatkCache.get(itemId);
		if (atk != null) {
			return atk.intValue();
		}
		final MapleData data = this.getItemData(itemId);
		atk = Integer.valueOf(MapleDataTool.getInt("info/incPAD", data, 0));
		this.projectileWatkCache.put(itemId, atk);
		return atk.intValue();
	}

	public String getName(int itemId) {
		if (this.nameCache.containsKey(itemId)) {
			return this.nameCache.get(itemId);
		}
		final MapleData strings = this.getStringData(itemId);
		if (strings == null) {
			return null;
		}
		final String ret = MapleDataTool.getString("name", strings, null);
		this.nameCache.put(itemId, ret);
		return ret;
	}

	public String getMsg(int itemId) {
		if (this.msgCache.containsKey(itemId)) {
			return this.msgCache.get(itemId);
		}
		final MapleData strings = this.getStringData(itemId);
		if (strings == null) {
			return null;
		}
		final String ret = MapleDataTool.getString("msg", strings, null);
		this.msgCache.put(itemId, ret);
		return ret;
	}

	public boolean isDropRestricted(int itemId) {
		if (this.dropRestrictionCache.containsKey(itemId)) {
			return this.dropRestrictionCache.get(itemId);
		}
		final MapleData data = this.getItemData(itemId);
		boolean bRestricted = MapleDataTool.getIntConvert("info/tradeBlock",
				data, 0) == 1;
		if (!bRestricted) {
			bRestricted = MapleDataTool.getIntConvert("info/quest", data, 0) == 1;
		}
		this.dropRestrictionCache.put(itemId, bRestricted);
		return bRestricted;
	}

	public boolean isPickupRestricted(int itemId) {
		if (this.pickupRestrictionCache.containsKey(itemId)) {
			return this.pickupRestrictionCache.get(itemId);
		}
		final MapleData data = this.getItemData(itemId);
		final boolean bRestricted = MapleDataTool.getIntConvert("info/only",
				data, 0) == 1;
		this.pickupRestrictionCache.put(itemId, bRestricted);
		return bRestricted;
	}

	public Map<String, Integer> getSkillStats(int itemId, double playerJob) {
		final Map<String, Integer> ret = new LinkedHashMap<>();
		final MapleData item = this.getItemData(itemId);
		if (item == null) {
			return null;
		}
		final MapleData info = item.getChildByPath("info");
		if (info == null) {
			return null;
		}
		for (final MapleData data : info.getChildren()) {
			if (data.getName().startsWith("inc")) {
				ret.put(data.getName().substring(3),
						MapleDataTool.getIntConvert(data));
			}
		}
		ret.put("masterLevel", MapleDataTool.getInt("masterLevel", info, 0));
		ret.put("reqSkillLevel", MapleDataTool.getInt("reqSkillLevel", info, 0));
		ret.put("success", MapleDataTool.getInt("success", info, 0));
		final MapleData skill = info.getChildByPath("skill");
		int curskill;
		for (int i = 0; i < skill.getChildren().size(); i++) {
			curskill = MapleDataTool.getInt(Integer.toString(i), skill, 0);
			if (curskill == 0) {
				break;
			}
			if ((curskill / 10000) == playerJob) {
				ret.put("skillid", curskill);
				break;
			}
		}
		if (ret.get("skillid") == null) {
			ret.put("skillid", 0);
		}
		return ret;
	}

	public List<Integer> petsCanConsume(int itemId) {
		final List<Integer> ret = new ArrayList<>();
		final MapleData data = this.getItemData(itemId);
		int curPetId;
		for (int i = 0; i < data.getChildren().size(); i++) {
			curPetId = MapleDataTool.getInt("spec/" + Integer.toString(i),
					data, 0);
			if (curPetId == 0) {
				break;
			}
			ret.add(Integer.valueOf(curPetId));
		}
		return ret;
	}

	public boolean isQuestItem(int itemId) {
		if (this.isQuestItemCache.containsKey(itemId)) {
			return this.isQuestItemCache.get(itemId);
		}
		final MapleData data = this.getItemData(itemId);
		final boolean questItem = MapleDataTool.getIntConvert("info/quest",
				data, 0) == 1;
		this.isQuestItemCache.put(itemId, questItem);
		return questItem;
	}

	public int getQuestIdFromItem(int itemId) {
		final MapleData data = this.getItemData(itemId);
		final int questItem = MapleDataTool
				.getIntConvert("info/quest", data, 0);
		return questItem;
	}

	private void loadCardIdData() {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = DatabaseConnection.getConnection().prepareStatement(
					"SELECT cardid, mobid FROM monstercarddata");
			rs = ps.executeQuery();
			while (rs.next()) {
				this.monsterBookID.put(rs.getInt(1), rs.getInt(2));
			}
			rs.close();
			ps.close();
		} catch (final SQLException e) {
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (ps != null) {
					ps.close();
				}
			} catch (final SQLException e) {
			}
		}
	}

	public int getCardMobId(int id) {
		return this.monsterBookID.get(id);
	}

	public boolean isUntradeableOnEquip(int itemId) {
		if (this.onEquipUntradableCache.containsKey(itemId)) {
			return this.onEquipUntradableCache.get(itemId);
		}
		final boolean untradableOnEquip = MapleDataTool.getIntConvert(
				"info/equipTradeBlock", this.getItemData(itemId), 0) > 0;
		this.onEquipUntradableCache.put(itemId, untradableOnEquip);
		return untradableOnEquip;
	}

	public scriptedItem getScriptedItemInfo(int itemId) {
		if (this.scriptedItemCache.containsKey(itemId)) {
			return this.scriptedItemCache.get(itemId);
		}
		if ((itemId / 10000) != 243) {
			return null;
		}
		final scriptedItem script = new scriptedItem(MapleDataTool.getInt(
				"spec/npc", this.getItemData(itemId), 0),
				MapleDataTool.getString("spec/script",
						this.getItemData(itemId), ""), MapleDataTool.getInt(
						"spec/runOnPickup", this.getItemData(itemId), 0) == 1);
		this.scriptedItemCache.put(itemId, script);
		return this.scriptedItemCache.get(itemId);
	}

	public boolean isKarmaAble(int itemId) {
		if (this.karmaCache.containsKey(itemId)) {
			return this.karmaCache.get(itemId);
		}
		final boolean bRestricted = MapleDataTool.getIntConvert(
				"info/tradeAvailable", this.getItemData(itemId), 0) > 0;
		this.karmaCache.put(itemId, bRestricted);
		return bRestricted;
	}

	public int getStateChangeItem(int itemId) {
		if (this.triggerItemCache.containsKey(itemId)) {
			return this.triggerItemCache.get(itemId);
		} else {
			final int triggerItem = MapleDataTool.getIntConvert(
					"info/stateChangeItem", this.getItemData(itemId), 0);
			this.triggerItemCache.put(itemId, triggerItem);
			return triggerItem;
		}
	}

	public int getExpById(int itemId) {
		if (this.expCache.containsKey(itemId)) {
			return this.expCache.get(itemId);
		} else {
			final int exp = MapleDataTool.getIntConvert("spec/exp",
					this.getItemData(itemId), 0);
			this.expCache.put(itemId, exp);
			return exp;
		}
	}

	public int getMaxLevelById(int itemId) {
		if (this.levelCache.containsKey(itemId)) {
			return this.levelCache.get(itemId);
		} else {
			final int level = MapleDataTool.getIntConvert("info/maxLevel",
					this.getItemData(itemId), 256);
			this.levelCache.put(itemId, level);
			return level;
		}
	}

	public Pair<Integer, List<RewardItem>> getItemReward(int itemId) {// Thanks
																		// Celino,
																		// used
																		// some
																		// stuffs
																		// :)
		if (this.rewardCache.containsKey(itemId)) {
			return this.rewardCache.get(itemId);
		}
		int totalprob = 0;
		final List<RewardItem> rewards = new ArrayList();
		for (final MapleData child : this.getItemData(itemId)
				.getChildByPath("reward").getChildren()) {
			final RewardItem reward = new RewardItem();
			reward.itemid = MapleDataTool.getInt("item", child, 0);
			reward.prob = (byte) MapleDataTool.getInt("prob", child, 0);
			reward.quantity = (short) MapleDataTool.getInt("count", child, 0);
			reward.effect = MapleDataTool.getString("Effect", child, "");
			reward.worldmsg = MapleDataTool.getString("worldMsg", child, null);
			reward.period = MapleDataTool.getInt("period", child, -1);

			totalprob += reward.prob;

			rewards.add(reward);
		}
		final Pair<Integer, List<RewardItem>> hmm = new Pair(totalprob, rewards);
		this.rewardCache.put(itemId, hmm);
		return hmm;
	}

	public boolean isConsumeOnPickup(int itemId) {
		if (this.consumeOnPickupCache.containsKey(itemId)) {
			return this.consumeOnPickupCache.get(itemId);
		}
		final MapleData data = this.getItemData(itemId);
		final boolean consume = (MapleDataTool.getIntConvert(
				"spec/consumeOnPickup", data, 0) == 1)
				|| (MapleDataTool.getIntConvert("specEx/consumeOnPickup", data,
						0) == 1);
		this.consumeOnPickupCache.put(itemId, consume);
		return consume;
	}

	public final boolean isTwoHanded(int itemId) {
		switch (this.getWeaponType(itemId)) {
		case AXE2H:
		case BLUNT2H:
		case BOW:
		case CLAW:
		case CROSSBOW:
		case POLE_ARM:
		case SPEAR:
		case SWORD2H:
		case GUN:
		case KNUCKLE:
			return true;
		default:
			return false;
		}
	}

	public boolean isCash(int itemId) {
		return ((itemId / 1000000) == 5)
				|| (this.getEquipStats(itemId).get("cash") == 1);
	}

	public Collection<Item> canWearEquipment(MapleCharacter chr,
			Collection<Item> items) {
		final MapleInventory inv = chr
				.getInventory(MapleInventoryType.EQUIPPED);
		if (inv.checked()) {
			return items;
		}
		final Collection<Item> itemz = new LinkedList<>();
		if ((chr.getJob() == MapleJob.SUPERGM) || (chr.getJob() == MapleJob.GM)) {
			for (final Item item : items) {
				final Equip equip = (Equip) item;
				equip.wear(true);
				itemz.add(item);
			}
			return itemz;
		}
		final boolean highfivestamp = false;
		/*
		 * Removed because players shouldn't even get this, and gm's should just
		 * be gm job. try { for (Pair<Item, MapleInventoryType> ii :
		 * ItemFactory.INVENTORY.loadItems(chr.getId(), false)) { if
		 * (ii.getRight() == MapleInventoryType.CASH) { if
		 * (ii.getLeft().getItemId() == 5590000) { highfivestamp = true; } } } }
		 * catch (SQLException ex) { }
		 */
		int tdex = chr.getDex(), tstr = chr.getStr(), tint = chr.getInt(), tluk = chr
				.getLuk();
		final int fame = chr.getFame();
		if ((chr.getJob() != MapleJob.SUPERGM) || (chr.getJob() != MapleJob.GM)) {
			for (final Item item : inv.list()) {
				final Equip equip = (Equip) item;
				tdex += equip.getDex();
				tstr += equip.getStr();
				tluk += equip.getLuk();
				tint += equip.getInt();
			}
		}
		for (final Item item : items) {
			final Equip equip = (Equip) item;
			int reqLevel = this.getEquipStats(equip.getItemId())
					.get("reqLevel");
			if (highfivestamp) {
				reqLevel -= 5;
				if (reqLevel < 0) {
					reqLevel = 0;
				}
			}
			/*
			 * int reqJob = getEquipStats(equip.getItemId()).get("reqJob"); if
			 * (reqJob != 0) { Really hard check, and not really needed in this
			 * one Gm's should just be GM job, and players cannot change jobs. }
			 */
			if (reqLevel > chr.getLevel()) {
				continue;
			} else if (this.getEquipStats(equip.getItemId()).get("reqDEX") > tdex) {
				continue;
			} else if (this.getEquipStats(equip.getItemId()).get("reqSTR") > tstr) {
				continue;
			} else if (this.getEquipStats(equip.getItemId()).get("reqLUK") > tluk) {
				continue;
			} else if (this.getEquipStats(equip.getItemId()).get("reqINT") > tint) {
				continue;
			}
			final int reqPOP = this.getEquipStats(equip.getItemId()).get(
					"reqPOP");
			if (reqPOP > 0) {
				if (this.getEquipStats(equip.getItemId()).get("reqPOP") > fame) {
					continue;
				}
			}
			equip.wear(true);
			itemz.add(equip);
		}
		inv.checked(true);
		return itemz;
	}

	public boolean canWearEquipment(MapleCharacter chr, Equip equip) {
		if ((chr.getJob() == MapleJob.SUPERGM) || (chr.getJob() == MapleJob.GM)) {
			equip.wear(true);
			return true;
		}
		final boolean highfivestamp = false;
		/*
		 * Removed check above for message >< try { for (Pair<Item,
		 * MapleInventoryType> ii : ItemFactory.INVENTORY.loadItems(chr.getId(),
		 * false)) { if (ii.getRight() == MapleInventoryType.CASH) { if
		 * (ii.getLeft().getItemId() == 5590000) { highfivestamp = true; } } } }
		 * catch (SQLException ex) { }
		 */
		int tdex = chr.getDex(), tstr = chr.getStr(), tint = chr.getInt(), tluk = chr
				.getLuk();
		for (final Item item : chr.getInventory(MapleInventoryType.EQUIPPED)
				.list()) {
			final Equip eq = (Equip) item;
			tdex += eq.getDex();
			tstr += eq.getStr();
			tluk += eq.getLuk();
			tint += eq.getInt();
		}
		int reqLevel = this.getEquipStats(equip.getItemId()).get("reqLevel");
		if (highfivestamp) {
			reqLevel -= 5;
		}
		int i = 0; // lol xD
		// Removed job check. Shouldn't really be needed.
		if (reqLevel > chr.getLevel()) {
			i++;
		} else if (this.getEquipStats(equip.getItemId()).get("reqDEX") > tdex) {
			i++;
		} else if (this.getEquipStats(equip.getItemId()).get("reqSTR") > tstr) {
			i++;
		} else if (this.getEquipStats(equip.getItemId()).get("reqLUK") > tluk) {
			i++;
		} else if (this.getEquipStats(equip.getItemId()).get("reqINT") > tint) {
			i++;
		}
		final int reqPOP = this.getEquipStats(equip.getItemId()).get("reqPOP");
		if (reqPOP > 0) {
			if (this.getEquipStats(equip.getItemId()).get("reqPOP") > chr
					.getFame()) {
				i++;
			}
		}

		if (i > 0) {
			equip.wear(false);
			return false;
		}
		equip.wear(true);
		return true;
	}

	public List<Pair<String, Integer>> getItemLevelupStats(int itemId,
			int level, boolean timeless) {
		final List<Pair<String, Integer>> list = new LinkedList<>();
		final MapleData data = this.getItemData(itemId);
		final MapleData data1 = data.getChildByPath("info").getChildByPath(
				"level");
		/*
		 * if ((timeless && level == 5) || (!timeless && level == 3)) {
		 * MapleData skilldata =
		 * data1.getChildByPath("case").getChildByPath("1")
		 * .getChildByPath(timeless ? "6" : "4"); if (skilldata != null) {
		 * List<MapleData> skills =
		 * skilldata.getChildByPath("Skill").getChildren(); for (int i = 0; i <
		 * skilldata.getChildByPath("Skill").getChildren().size(); i++) {
		 * System.
		 * out.println(MapleDataTool.getInt(skills.get(i).getChildByPath("id"
		 * ))); if (Math.random() < 0.1) list.add(new Pair<String,
		 * Integer>("Skill" + 0,
		 * MapleDataTool.getInt(skills.get(i).getChildByPath("id")))); } } }
		 */
		if (data1 != null) {
			final MapleData data2 = data1.getChildByPath("info")
					.getChildByPath(Integer.toString(level));
			if (data2 != null) {
				for (final MapleData da : data2.getChildren()) {
					if (Math.random() < 0.9) {
						if (da.getName().startsWith("incDEXMin")) {
							list.add(new Pair<>("incDEX", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incDEXMax")))));
						} else if (da.getName().startsWith("incSTRMin")) {
							list.add(new Pair<>("incSTR", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incSTRMax")))));
						} else if (da.getName().startsWith("incINTMin")) {
							list.add(new Pair<>("incINT", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incINTMax")))));
						} else if (da.getName().startsWith("incLUKMin")) {
							list.add(new Pair<>("incLUK", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incLUKMax")))));
						} else if (da.getName().startsWith("incMHPMin")) {
							list.add(new Pair<>("incMHP", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incMHPMax")))));
						} else if (da.getName().startsWith("incMMPMin")) {
							list.add(new Pair<>("incMMP", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incMMPMax")))));
						} else if (da.getName().startsWith("incPADMin")) {
							list.add(new Pair<>("incPAD", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incPADMax")))));
						} else if (da.getName().startsWith("incMADMin")) {
							list.add(new Pair<>("incMAD", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incMADMax")))));
						} else if (da.getName().startsWith("incPDDMin")) {
							list.add(new Pair<>("incPDD", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incPDDMax")))));
						} else if (da.getName().startsWith("incMDDMin")) {
							list.add(new Pair<>("incMDD", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incMDDMax")))));
						} else if (da.getName().startsWith("incACCMin")) {
							list.add(new Pair<>("incACC", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incACCMax")))));
						} else if (da.getName().startsWith("incEVAMin")) {
							list.add(new Pair<>("incEVA", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incEVAMax")))));
						} else if (da.getName().startsWith("incSpeedMin")) {
							list.add(new Pair<>("incSpeed", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incSpeedMax")))));
						} else if (da.getName().startsWith("incJumpMin")) {
							list.add(new Pair<>("incJump", Randomizer.rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incJumpMax")))));
						}
					}
				}
			}
		}

		return list;
	}

	public class scriptedItem {

		private final boolean runOnPickup;
		private final int npc;
		private final String script;

		public scriptedItem(int npc, String script, boolean rop) {
			this.npc = npc;
			this.script = script;
			this.runOnPickup = rop;
		}

		public int getNpc() {
			return this.npc;
		}

		public String getScript() {
			return this.script;
		}

		public boolean runOnPickup() {
			return this.runOnPickup;
		}
	}

	public static final class RewardItem {

		public int itemid, period;
		public short prob, quantity;
		public String effect, worldmsg;
	}
}