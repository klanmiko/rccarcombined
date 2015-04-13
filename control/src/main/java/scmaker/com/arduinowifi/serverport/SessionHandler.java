package scmaker.com.arduinowifi.serverport;

import java.net.SocketAddress;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class SessionHandler extends ChannelInboundHandlerAdapter{
	private long timereceived;
	private final long timeout = 2000;
	@Override
	public void channelRegistered(ChannelHandlerContext ctx)
	{
		timereceived = System.currentTimeMillis();
		Server.addClient(ctx.channel());
		System.out.println(ctx.channel().remoteAddress().toString());
		ctx.fireChannelRegistered();
	}
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx)
	{
		Server.removeClient(ctx.channel());
		ctx.close();
	}
}
