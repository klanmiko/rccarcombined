package scmaker.com.arduinowifi.serverport;

import io.netty.channel.Channel;

public class SpamThread extends Thread implements Runnable{
    private int speed,steer;
	public SpamThread() {
		// TODO Auto-generated constructor stub
	}
	public void run()
	{
		while(true)
		{
			for(Channel c : Server.getClients())
			{
				c.writeAndFlush(new DataClass(steer,speed));
			}
			try {
				synchronized(this)
				{
					this.wait(1);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setSteer(int steer) {
        this.steer = steer;
    }
}
