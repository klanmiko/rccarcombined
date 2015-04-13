package scmaker.com.arduinowifi.serverport;

import io.netty.bootstrap.ChannelFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server{
	private int port;
    private static SpamThread spam;
        private static List<Channel> clients = Collections.synchronizedList(new ArrayList<Channel>());
	public Server(int port)
	{
		this.port = port;
	}
	public void run() throws Exception {
		spam = new SpamThread();
        spam.start();
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) // (3)
             .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                	 ch.pipeline().addLast(new SessionHandler());                	 
                	 ch.pipeline().addLast(new SinkHole());
                	 ch.pipeline().addLast(new DataClassEncoder());
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)          // (5)
             .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)
    
            // Bind and start to accept incoming connections.
            System.out.print("Server Initialised\n");
            System.out.flush();
            ChannelFuture f = b.bind(port).sync(); // (7)
            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
	public static void addClient(Channel remoteAddress) {
        synchronized (clients) {
            clients.add(remoteAddress);
        }
	}
	public static void removeClient(Channel remoteAddress) {
        synchronized (clients) {
            clients.remove(remoteAddress);
        }
	}
	public static List<Channel> getClients()
	{
        synchronized (clients) {
            return clients;
        }
	}
    public static void setSteer(int steer)
    {
        spam.setSteer(steer);
    }
    public static void setSpeed(int speed)
    {
        spam.setSpeed(speed);
    }
}
