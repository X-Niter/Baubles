package baubles.common.network;

import baubles.api.BaublesApi;
import baubles.api.cap.BaublesContainer;
import baubles.api.cap.IBaublesItemHandler;
import baubles.common.Baubles;
import baubles.common.Config;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;
import java.util.SortedMap;

public class PacketSync implements IMessage {

	int playerId;

	private int entityId;

	private int slotId;

	private ItemStack stack;
	byte slot=0;
	ItemStack bauble;

	private SortedMap<String, BaublesContainer> map;

	private String baubleId;

	public PacketSync() {}

	public PacketSync(EntityLivingBase p, int slot, ItemStack bauble) {
		this.slot = (byte) slot;
		this.bauble = bauble;
		this.playerId = p.getEntityId();
	}
	public PacketSync(int entityId, String baubleId, int slotId, ItemStack stack) {
		this.entityId = entityId;
		this.slotId = slotId;
		this.stack = stack.copy();
		this.baubleId = baubleId;
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeInt(playerId);
		buffer.writeByte(slot);
		ByteBufUtils.writeItemStack(buffer, bauble);
	}

	public static void encode(PacketSync msg, PacketBuffer buf) {
		if (Config.useCurioGUI) {
			buf.writeInt(msg.entityId);
			buf.writeString(msg.baubleId);
			buf.writeInt(msg.slotId);
			buf.writeItemStack(msg.stack);
		}
	}

	public static PacketSync decode(PacketBuffer buf) throws IOException {
		return new PacketSync(buf.readInt(), buf.readString(25), buf.readInt(), buf.readItemStack());
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		playerId = buffer.readInt();
		slot = buffer.readByte();
		bauble = ByteBufUtils.readItemStack(buffer);
	}

	public static class Handler implements IMessageHandler<PacketSync, IMessage>
	{
		@Override
		public IMessage onMessage(PacketSync message, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(new Runnable(){ public void run() {
				World world = Baubles.proxy.getClientWorld();
				if (world == null) {
					return;
				}
				Entity p = world.getEntityByID(message.playerId);
				if (p instanceof EntityPlayer) {
					IBaublesItemHandler baubles = BaublesApi.getBaublesHandler((EntityPlayer) p);
					baubles.setStackInSlot(message.slot, message.bauble);
				}
				if (Config.useCurioGUI) {
					if (p instanceof EntityLivingBase) {
						BaublesApi.getOBaublesHandler((EntityLivingBase) p).ifPresent(handler -> handler.setStackInSlot(message.baubleId, message.slotId, message.stack));
					}
				}
			}});
			return null;
		}
	}
}
