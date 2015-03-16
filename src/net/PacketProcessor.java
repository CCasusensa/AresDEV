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
package net;

import java.util.LinkedHashMap;
import java.util.Map;

import net.server.channel.handlers.*;
import net.server.handlers.CustomPacketHandler;
import net.server.handlers.KeepAliveHandler;
import net.server.handlers.LoginRequiringNoOpHandler;
import net.server.handlers.login.AcceptToSHandler;
import net.server.handlers.login.AfterLoginHandler;
import net.server.handlers.login.CharSelectedHandler;
import net.server.handlers.login.CharSelectedWithPicHandler;
import net.server.handlers.login.CharlistRequestHandler;
import net.server.handlers.login.CheckCharNameHandler;
import net.server.handlers.login.CreateCharHandler;
import net.server.handlers.login.DeleteCharHandler;
import net.server.handlers.login.GuestLoginHandler;
import net.server.handlers.login.LoginPasswordHandler;
import net.server.handlers.login.PickCharHandler;
import net.server.handlers.login.RegisterPicHandler;
import net.server.handlers.login.RegisterPinHandler;
import net.server.handlers.login.RelogRequestHandler;
import net.server.handlers.login.ServerStatusRequestHandler;
import net.server.handlers.login.ServerlistRequestHandler;
import net.server.handlers.login.SetGenderHandler;
import net.server.handlers.login.ViewAllCharSelectedWithPicHandler;
import net.server.handlers.login.ViewAllPicRegisterHandler;
import net.server.handlers.login.ViewCharHandler;

public final class PacketProcessor {

	private final static Map<String, PacketProcessor> instances = new LinkedHashMap<>();
	private MaplePacketHandler[] handlers;

	private PacketProcessor() {
		int maxRecvOp = 0;
		for (final RecvOpcode op : RecvOpcode.values()) {
			if (op.getValue() > maxRecvOp) {
				maxRecvOp = op.getValue();
			}
		}
		this.handlers = new MaplePacketHandler[maxRecvOp + 1];
	}

	public MaplePacketHandler getHandler(short packetId) {
		if (packetId > this.handlers.length) {
			return null;
		}
		final MaplePacketHandler handler = this.handlers[packetId];
		if (handler != null) {
			return handler;
		}
		return null;
	}

	public void registerHandler(RecvOpcode code, MaplePacketHandler handler) {
		try {
			this.handlers[code.getValue()] = handler;
		} catch (final ArrayIndexOutOfBoundsException e) {
			System.out.println("Error registering handler - " + code.name());
		}
	}

	public synchronized static PacketProcessor getProcessor(int world,
			int channel) {
		final String lolpair = world + " " + channel;
		PacketProcessor processor = instances.get(lolpair);
		if (processor == null) {
			processor = new PacketProcessor();
			processor.reset(channel);
			instances.put(lolpair, processor);
		}
		return processor;
	}

	public void reset(int channel) {
		this.handlers = new MaplePacketHandler[this.handlers.length];

		this.registerHandler(RecvOpcode.PONG, new KeepAliveHandler());
		this.registerHandler(RecvOpcode.CUSTOM_PACKET,
				new CustomPacketHandler());
		if (channel < 0) {// login
			this.registerHandler(RecvOpcode.ACCEPT_TOS, new AcceptToSHandler());
			this.registerHandler(RecvOpcode.AFTER_LOGIN,
					new AfterLoginHandler());
			this.registerHandler(RecvOpcode.SERVERLIST_REREQUEST,
					new ServerlistRequestHandler());
			this.registerHandler(RecvOpcode.CHARLIST_REQUEST,
					new CharlistRequestHandler());
			this.registerHandler(RecvOpcode.CHAR_SELECT,
					new CharSelectedHandler());
			this.registerHandler(RecvOpcode.LOGIN_PASSWORD,
					new LoginPasswordHandler());
			this.registerHandler(RecvOpcode.RELOG, new RelogRequestHandler());
			this.registerHandler(RecvOpcode.SERVERLIST_REQUEST,
					new ServerlistRequestHandler());
			this.registerHandler(RecvOpcode.SERVERSTATUS_REQUEST,
					new ServerStatusRequestHandler());
			this.registerHandler(RecvOpcode.CHECK_CHAR_NAME,
					new CheckCharNameHandler());
			this.registerHandler(RecvOpcode.CREATE_CHAR,
					new CreateCharHandler());
			this.registerHandler(RecvOpcode.DELETE_CHAR,
					new DeleteCharHandler());
			this.registerHandler(RecvOpcode.VIEW_ALL_CHAR,
					new ViewCharHandler());
			this.registerHandler(RecvOpcode.PICK_ALL_CHAR,
					new PickCharHandler());
			this.registerHandler(RecvOpcode.REGISTER_PIN,
					new RegisterPinHandler());
			this.registerHandler(RecvOpcode.GUEST_LOGIN,
					new GuestLoginHandler());
			this.registerHandler(RecvOpcode.REGISTER_PIC,
					new RegisterPicHandler());
			this.registerHandler(RecvOpcode.CHAR_SELECT_WITH_PIC,
					new CharSelectedWithPicHandler());
			this.registerHandler(RecvOpcode.SET_GENDER, new SetGenderHandler());
			this.registerHandler(RecvOpcode.VIEW_ALL_WITH_PIC,
					new ViewAllCharSelectedWithPicHandler());
			this.registerHandler(RecvOpcode.VIEW_ALL_PIC_REGISTER,
					new ViewAllPicRegisterHandler());
		} else {
			// CHANNEL HANDLERS
			this.registerHandler(RecvOpcode.CHANGE_CHANNEL,
					new ChangeChannelHandler());
			this.registerHandler(RecvOpcode.STRANGE_DATA,
					LoginRequiringNoOpHandler.getInstance());
			this.registerHandler(RecvOpcode.GENERAL_CHAT,
					new GeneralChatHandler());
			this.registerHandler(RecvOpcode.WHISPER, new WhisperHandler());
			this.registerHandler(RecvOpcode.NPC_TALK, new NPCTalkHandler());
			this.registerHandler(RecvOpcode.NPC_TALK_MORE,
					new NPCMoreTalkHandler());
			this.registerHandler(RecvOpcode.QUEST_ACTION,
					new QuestActionHandler());
			this.registerHandler(RecvOpcode.NPC_SHOP, new NPCShopHandler());
			this.registerHandler(RecvOpcode.ITEM_SORT, new ItemSortHandler());
			this.registerHandler(RecvOpcode.ITEM_MOVE, new ItemMoveHandler());
			this.registerHandler(RecvOpcode.MESO_DROP, new MesoDropHandler());
			this.registerHandler(RecvOpcode.PLAYER_LOGGEDIN,
					new PlayerLoggedinHandler());
			this.registerHandler(RecvOpcode.CHANGE_MAP, new ChangeMapHandler());
			this.registerHandler(RecvOpcode.MOVE_LIFE, new MoveLifeHandler());
			this.registerHandler(RecvOpcode.CLOSE_RANGE_ATTACK,
					new CloseRangeDamageHandler());
			this.registerHandler(RecvOpcode.RANGED_ATTACK,
					new RangedAttackHandler());
			this.registerHandler(RecvOpcode.MAGIC_ATTACK,
					new MagicDamageHandler());
			this.registerHandler(RecvOpcode.TAKE_DAMAGE,
					new TakeDamageHandler());
			this.registerHandler(RecvOpcode.MOVE_PLAYER,
					new MovePlayerHandler());
			this.registerHandler(RecvOpcode.USE_CASH_ITEM,
					new UseCashItemHandler());
			this.registerHandler(RecvOpcode.USE_ITEM, new UseItemHandler());
			this.registerHandler(RecvOpcode.USE_RETURN_SCROLL,
					new UseItemHandler());
			this.registerHandler(RecvOpcode.USE_UPGRADE_SCROLL,
					new ScrollHandler());
			this.registerHandler(RecvOpcode.USE_SUMMON_BAG, new UseSummonBag());
			this.registerHandler(RecvOpcode.FACE_EXPRESSION,
					new FaceExpressionHandler());
			this.registerHandler(RecvOpcode.HEAL_OVER_TIME,
					new HealOvertimeHandler());
			this.registerHandler(RecvOpcode.ITEM_PICKUP,
					new ItemPickupHandler());
			this.registerHandler(RecvOpcode.CHAR_INFO_REQUEST,
					new CharInfoRequestHandler());
			this.registerHandler(RecvOpcode.SPECIAL_MOVE,
					new SpecialMoveHandler());
			this.registerHandler(RecvOpcode.USE_INNER_PORTAL,
					new InnerPortalHandler());
			this.registerHandler(RecvOpcode.CANCEL_BUFF,
					new CancelBuffHandler());
			this.registerHandler(RecvOpcode.CANCEL_ITEM_EFFECT,
					new CancelItemEffectHandler());
			this.registerHandler(RecvOpcode.PLAYER_INTERACTION,
					new PlayerInteractionHandler());
			this.registerHandler(RecvOpcode.DISTRIBUTE_AP,
					new DistributeAPHandler());
			this.registerHandler(RecvOpcode.DISTRIBUTE_SP,
					new DistributeSPHandler());
			this.registerHandler(RecvOpcode.CHANGE_KEYMAP,
					new KeymapChangeHandler());
			this.registerHandler(RecvOpcode.CHANGE_MAP_SPECIAL,
					new ChangeMapSpecialHandler());
			this.registerHandler(RecvOpcode.STORAGE, new StorageHandler());
			this.registerHandler(RecvOpcode.GIVE_FAME, new GiveFameHandler());
			this.registerHandler(RecvOpcode.PARTY_OPERATION,
					new PartyOperationHandler());
			this.registerHandler(RecvOpcode.DENY_PARTY_REQUEST,
					new DenyPartyRequestHandler());
			this.registerHandler(RecvOpcode.PARTYCHAT, new PartyChatHandler());
			this.registerHandler(RecvOpcode.USE_DOOR, new DoorHandler());
			this.registerHandler(RecvOpcode.ENTER_MTS, new EnterMTSHandler());
			this.registerHandler(RecvOpcode.ENTER_CASHSHOP,
					new EnterCashShopHandler());
			this.registerHandler(RecvOpcode.DAMAGE_SUMMON,
					new DamageSummonHandler());
			this.registerHandler(RecvOpcode.MOVE_SUMMON,
					new MoveSummonHandler());
			this.registerHandler(RecvOpcode.SUMMON_ATTACK,
					new SummonDamageHandler());
			this.registerHandler(RecvOpcode.BUDDYLIST_MODIFY,
					new BuddylistModifyHandler());
			this.registerHandler(RecvOpcode.USE_ITEMEFFECT,
					new UseItemEffectHandler());
			this.registerHandler(RecvOpcode.USE_CHAIR, new UseChairHandler());
			this.registerHandler(RecvOpcode.CANCEL_CHAIR,
					new CancelChairHandler());
			this.registerHandler(RecvOpcode.DAMAGE_REACTOR,
					new ReactorHitHandler());
			this.registerHandler(RecvOpcode.GUILD_OPERATION,
					new GuildOperationHandler());
			this.registerHandler(RecvOpcode.DENY_GUILD_REQUEST,
					new DenyGuildRequestHandler());
			this.registerHandler(RecvOpcode.BBS_OPERATION,
					new BBSOperationHandler());
			this.registerHandler(RecvOpcode.SKILL_EFFECT,
					new SkillEffectHandler());
			this.registerHandler(RecvOpcode.MESSENGER, new MessengerHandler());
			this.registerHandler(RecvOpcode.NPC_ACTION, new NPCAnimation());
			this.registerHandler(RecvOpcode.CHECK_CASH,
					new TouchingCashShopHandler());
			this.registerHandler(RecvOpcode.CASHSHOP_OPERATION,
					new CashOperationHandler());
			this.registerHandler(RecvOpcode.COUPON_CODE,
					new CouponCodeHandler());
			this.registerHandler(RecvOpcode.SPAWN_PET, new SpawnPetHandler());
			this.registerHandler(RecvOpcode.MOVE_PET, new MovePetHandler());
			this.registerHandler(RecvOpcode.PET_CHAT, new PetChatHandler());
			this.registerHandler(RecvOpcode.PET_COMMAND,
					new PetCommandHandler());
			this.registerHandler(RecvOpcode.PET_FOOD, new PetFoodHandler());
			this.registerHandler(RecvOpcode.PET_LOOT, new PetLootHandler());
			this.registerHandler(RecvOpcode.AUTO_AGGRO, new AutoAggroHandler());
			this.registerHandler(RecvOpcode.MONSTER_BOMB,
					new MonsterBombHandler());
			this.registerHandler(RecvOpcode.CANCEL_DEBUFF,
					new CancelDebuffHandler());
			this.registerHandler(RecvOpcode.USE_SKILL_BOOK,
					new SkillBookHandler());
			this.registerHandler(RecvOpcode.SKILL_MACRO,
					new SkillMacroHandler());
			this.registerHandler(RecvOpcode.NOTE_ACTION,
					new NoteActionHandler());
			this.registerHandler(RecvOpcode.CLOSE_CHALKBOARD,
					new CloseChalkboardHandler());
			this.registerHandler(RecvOpcode.USE_MOUNT_FOOD,
					new UseMountFoodHandler());
			this.registerHandler(RecvOpcode.MTS_OPERATION, new MTSHandler());
			this.registerHandler(RecvOpcode.RING_ACTION,
					new RingActionHandler());
			this.registerHandler(RecvOpcode.SPOUSE_CHAT,
					new SpouseChatHandler());
			this.registerHandler(RecvOpcode.PET_AUTO_POT,
					new PetAutoPotHandler());
			this.registerHandler(RecvOpcode.PET_EXCLUDE_ITEMS,
					new PetExcludeItemsHandler());
			this.registerHandler(RecvOpcode.TOUCH_MONSTER_ATTACK,
					new TouchMonsterDamageHandler());
			this.registerHandler(RecvOpcode.TROCK_ADD_MAP,
					new TrockAddMapHandler());
			this.registerHandler(RecvOpcode.HIRED_MERCHANT_REQUEST,
					new HiredMerchantRequest());
			this.registerHandler(RecvOpcode.MOB_DAMAGE_MOB,
					new MobDamageMobHandler());
			this.registerHandler(RecvOpcode.REPORT, new ReportHandler());
			this.registerHandler(RecvOpcode.MONSTER_BOOK_COVER,
					new MonsterBookCoverHandler());
			this.registerHandler(RecvOpcode.AUTO_DISTRIBUTE_AP,
					new AutoAssignHandler());
			this.registerHandler(RecvOpcode.MAKER_SKILL,
					new MakerSkillHandler());
			this.registerHandler(RecvOpcode.ADD_FAMILY, new FamilyAddHandler());
			this.registerHandler(RecvOpcode.USE_FAMILY, new FamilyUseHandler());
			this.registerHandler(RecvOpcode.USE_HAMMER, new UseHammerHandler());
			this.registerHandler(RecvOpcode.SCRIPTED_ITEM,
					new ScriptedItemHandler());
			this.registerHandler(RecvOpcode.TOUCHING_REACTOR,
					new TouchReactorHandler());
			this.registerHandler(RecvOpcode.BEHOLDER, new BeholderHandler());
			this.registerHandler(RecvOpcode.ADMIN_COMMAND,
					new AdminCommandHandler());
			this.registerHandler(RecvOpcode.ADMIN_LOG, new AdminLogHandler());
			this.registerHandler(RecvOpcode.ALLIANCE_OPERATION,
					new AllianceOperationHandler());
			this.registerHandler(RecvOpcode.USE_SOLOMON_ITEM,
					new UseSolomonHandler());
			this.registerHandler(RecvOpcode.USE_GACHA_EXP,
					new UseGachaExpHandler());
			this.registerHandler(RecvOpcode.USE_ITEM_REWARD,
					new ItemRewardHandler());
			this.registerHandler(RecvOpcode.USE_REMOTE,
					new RemoteGachaponHandler());
			this.registerHandler(RecvOpcode.ACCEPT_FAMILY,
					new AcceptFamilyHandler());
			this.registerHandler(RecvOpcode.DUEY_ACTION, new DueyHandler());
			this.registerHandler(RecvOpcode.USE_DEATHITEM,
					new UseDeathItemHandler());
			// registerHandler(RecvOpcode.PLAYER_UPDATE, new
			// PlayerUpdateHandler());don't use unused stuff
			this.registerHandler(RecvOpcode.USE_MAPLELIFE,
					new UseMapleLifeHandler());
			this.registerHandler(RecvOpcode.USE_CATCH_ITEM,
					new UseCatchItemHandler());
			this.registerHandler(RecvOpcode.MOB_DAMAGE_MOB_FRIENDLY,
					new MobDamageMobFriendlyHandler());
			this.registerHandler(RecvOpcode.PARTY_SEARCH_REGISTER,
					new PartySearchRegisterHandler());
			this.registerHandler(RecvOpcode.PARTY_SEARCH_START,
					new PartySearchStartHandler());
			this.registerHandler(RecvOpcode.ITEM_SORT2, new ItemIdSortHandler());
			this.registerHandler(RecvOpcode.LEFT_KNOCKBACK,
					new LeftKnockbackHandler());
			this.registerHandler(RecvOpcode.SNOWBALL, new SnowballHandler());
			this.registerHandler(RecvOpcode.COCONUT, new CoconutHandler());
			this.registerHandler(RecvOpcode.ARAN_COMBO_COUNTER,
					new AranComboHandler());
			this.registerHandler(RecvOpcode.CLICK_GUIDE,
					new ClickGuideHandler());
			this.registerHandler(RecvOpcode.FREDRICK_ACTION,
					new FredrickHandler());
			this.registerHandler(RecvOpcode.MONSTER_CARNIVAL,
					new MonsterCarnivalHandler());
			this.registerHandler(RecvOpcode.REMOTE_STORE,
					new RemoteStoreHandler());
			this.registerHandler(RecvOpcode.WEDDING_ACTION,
					new WeddingHandler());
			this.registerHandler(RecvOpcode.ADMIN_CHAT, new AdminChatHandler());
		}
	}
}