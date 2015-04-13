package scmaker.com.arduinowifi.serverport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class DataClassEncoder extends MessageToByteEncoder<DataClass>{

	public DataClassEncoder() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void encode(ChannelHandlerContext arg0, DataClass arg1,
			ByteBuf arg2) throws Exception {
		arg2.writeByte(SessionProtocol.startbyte);
		arg2.writeByte(SessionProtocol.steerbyte);
		if(arg1.steer<0)
		{
			arg2.writeByte(0x00);
			arg2.writeByte((byte)(Math.abs(arg1.steer)));
		}
		else{
			arg2.writeByte(0x01);
			arg2.writeByte((byte)arg1.steer);
		}
		arg2.writeByte(SessionProtocol.speedbyte);
		if(arg1.speed<0)
		{
			arg2.writeByte(0x00);
			arg2.writeByte((byte)(Math.abs(arg1.speed)));
		}
		else{
			arg2.writeByte(0x01);
			arg2.writeByte((byte)arg1.speed);
		}
		arg2.writeByte(SessionProtocol.delimbytes);
	}

}
