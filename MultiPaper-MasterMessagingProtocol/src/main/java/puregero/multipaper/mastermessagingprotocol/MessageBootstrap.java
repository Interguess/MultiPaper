package puregero.multipaper.mastermessagingprotocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import puregero.multipaper.mastermessagingprotocol.messages.Message;
import puregero.multipaper.mastermessagingprotocol.messages.Protocol;

import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

public class MessageBootstrap<I extends Message<?>, O extends Message<?>> extends ChannelInitializer<SocketChannel> {

    private static ThreadFactory eventLoopThreadFactory = new ThreadFactory() {
        private int counter = 0;

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "MultiPaper-Netty-" + (++counter));
            thread.setDaemon(true);
            return thread;
        }
    };

    private static EventLoopGroup eventLoopGroup;
    private static Class<? extends SocketChannel> socketChannelClass;
    private static Class<? extends ServerSocketChannel> serverSocketChannelClass;

    private final Protocol<I> inboundProtocol;
    private final Protocol<O> outboundProtocol;
    private final Consumer<SocketChannel> setupChannel;

    public static EventLoopGroup getEventLoopGroup() {
        if (eventLoopGroup == null) {
            if (Epoll.isAvailable()) {
                eventLoopGroup = new EpollEventLoopGroup(Integer.getInteger("multipaper.netty.threads", 0), eventLoopThreadFactory);
                socketChannelClass = EpollSocketChannel.class;
                serverSocketChannelClass = EpollServerSocketChannel.class;
            } else {
                eventLoopGroup = new NioEventLoopGroup(Integer.getInteger("multipaper.netty.threads", 0), eventLoopThreadFactory);
                socketChannelClass = NioSocketChannel.class;
                serverSocketChannelClass = NioServerSocketChannel.class;
            }
        }
        return eventLoopGroup;
    }

    public MessageBootstrap(Protocol<I> inboundProtocol, Protocol<O> outboundProtocol, Consumer<SocketChannel> setupChannel) {
        this.inboundProtocol = inboundProtocol;
        this.outboundProtocol = outboundProtocol;
        this.setupChannel = setupChannel;
    }

    private Bootstrap createBootstrap() {
        return new Bootstrap()
                .group(getEventLoopGroup())
                .channel(socketChannelClass)
                .handler(this)
                .option(ChannelOption.SO_KEEPALIVE, true);
    }

    private ServerBootstrap createServerBootstrap() {
        return new ServerBootstrap()
                .group(getEventLoopGroup())
                .channel(serverSocketChannelClass)
                .childHandler(this)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
    }

    public void connectTo(String address, int port, Consumer<Throwable> onFailure) {
        createBootstrap().connect(address, port).addListener(future -> {
            if (future.cause() != null) {
                onFailure.accept(future.cause());
            }
        });
    }

    public void listenOn(String address, int port, Consumer<Throwable> onFailure) {
        createServerBootstrap().bind(address, port).addListener(future -> {
            if (future.cause() != null) {
                onFailure.accept(future.cause());
            } else {
                System.out.println("Listening on " + address + ":" + port);
            }
        });
    }

    public void listenOn(int port, Consumer<Throwable> onFailure) {
        createServerBootstrap().bind(port).addListener(future -> {
            if (future.cause() != null) {
                onFailure.accept(future.cause());
            } else {
                System.out.println("Listening on port " + port);
            }
        });
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        socketChannel.pipeline().addLast(new MessageLengthEncoder());
        socketChannel.pipeline().addLast(new MessageLengthDecoder());
        socketChannel.pipeline().addLast(new MessageEncoder<>(this.outboundProtocol));
        socketChannel.pipeline().addLast(new MessageDecoder<>(this.inboundProtocol));
        setupChannel.accept(socketChannel);
    }

}
