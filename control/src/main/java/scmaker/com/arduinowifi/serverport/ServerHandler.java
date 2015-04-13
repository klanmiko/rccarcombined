package scmaker.com.arduinowifi.serverport;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class ServerHandler extends ChannelInboundHandlerAdapter { // (1)
	static Map<SocketAddress,Integer> messages = new HashMap<SocketAddress,Integer>();
	@Override
	public void channelActive(ChannelHandlerContext ctx)
	{
		ServerHandler.messages.put(ctx.channel().remoteAddress(),new Integer(0));
		System.out.print(ctx.channel().remoteAddress().toString()+" has connected\n");
		System.out.flush();
	}
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
        // Discard the received data silently.
        ByteBuf m = (ByteBuf)msg; // (3)
        try{
        	while(m.isReadable())
        	{
        		Integer n = messages.get(ctx.channel().remoteAddress());
        		n++;
        		messages.put(ctx.channel().remoteAddress(), n);
        		System.out.print((char)m.readByte()+"\n");
        		System.out.print(ctx.channel().remoteAddress().toString()+" has "+n+" messages\n");
        		System.out.flush();
        	}
        }
        finally{
        	ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx)
            throws Exception{
    	messages.remove(ctx.channel().remoteAddress());
    	System.out.print(ctx.channel().remoteAddress().toString()+" has disconnected");
    	System.out.flush();
    	ctx.close();
    }
}
