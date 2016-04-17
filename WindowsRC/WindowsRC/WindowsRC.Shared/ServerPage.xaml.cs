using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;

using Windows.Networking;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;
using System.Threading.Tasks;
using System.Threading;
using Windows.Networking.Sockets;
using Windows.Storage.Streams;
using System.Diagnostics;
using Windows.Networking.Connectivity;
using Windows.Storage;
using Windows.Storage.Pickers;
using Windows.Storage.Streams;
using Windows.Graphics.Imaging;
using Windows.UI.Xaml.Media.Imaging;
using Windows.UI.Core;
// The Blank Page item template is documented at http://go.microsoft.com/fwlink/?LinkId=234238
namespace WindowsRC
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class ServerPage : Page
    {
        private StreamSocketListener ssl,vsl;
        private StreamSocket ss,vs;
        private HostName hostname;
        private IBuffer x;
        private byte[] f;
        private IBuffer imagebuf;
        private uint sizei=0;
        private CancellationTokenSource ctsVertical;
        private CancellationTokenSource ctsHorizontal;
        private void connected(StreamSocketListener listener, StreamSocketListenerConnectionReceivedEventArgs e)
        {
            ss = e.Socket;
        }
        public ServerPage()
        {
            f = new byte[] { 0, 0, 0 };
            x = new Windows.Storage.Streams.Buffer(1);
            var icp = NetworkInformation.GetInternetConnectionProfile();
            if(icp!=null&&icp.NetworkAdapter!=null)
            {
                hostname =
         NetworkInformation.GetHostNames()
             .SingleOrDefault(
                 hn =>
                 hn.IPInformation != null && hn.IPInformation.NetworkAdapter != null
                 && hn.IPInformation.NetworkAdapter.NetworkAdapterId
                 == icp.NetworkAdapter.NetworkAdapterId);
            }
            this.InitializeComponent();
            ssl = new StreamSocketListener();
            ssl.Control.QualityOfService = SocketQualityOfService.LowLatency;
            ssl.ConnectionReceived += new TypedEventHandler<StreamSocketListener, StreamSocketListenerConnectionReceivedEventArgs>(connected);
            ssl.BindEndpointAsync(hostname,"6551");
            vsl = new StreamSocketListener();
            vsl.Control.QualityOfService = SocketQualityOfService.LowLatency;
            vsl.ConnectionReceived += new TypedEventHandler<StreamSocketListener, StreamSocketListenerConnectionReceivedEventArgs> (video_Connect);
            vsl.BindEndpointAsync(hostname,"56469");
        }
        private void smoothBreak(Slider bar, double end) {
            double start = bar.Value;
            while(!(bar.Value==end))
            {
                bar.Value=start+(end- start)*0.2;
            }
        }
        private async void video_Connect(StreamSocketListener l, StreamSocketListenerConnectionReceivedEventArgs e)
        {
            vs = e.Socket;
            await Task.Run(()=>videoFrame(e.Socket));
        }
        private void send_Value(object sender, RangeBaseValueChangedEventArgs e)
        {
            if (ss != null)
            {
                byte[] buffer = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
                buffer[0] = (byte)SessionProtocol.startbyte;
                buffer[1] = (byte)SessionProtocol.steerbyte;
                if (steer.Value < 0)
                {
                    buffer[2] = 0x00;
                    buffer[3] = (byte)(steer.Value * -1);
                }
                else if (steer.Value > 0)
                {
                    buffer[2] = 0x01;
                    buffer[3] = (byte)steer.Value;
                }
                buffer[4] = (byte)SessionProtocol.speedbyte;
                if (speed.Value < 0)
                {
                    buffer[5] = 0x00;
                    buffer[6] = (byte)(speed.Value * -1);
                }
                else if (speed.Value > 0)
                {
                    buffer[5] = 0x01;
                    buffer[6] = (byte)speed.Value;
                }
                buffer[7] = (byte)SessionProtocol.delimbytes;
                ss.OutputStream.WriteAsync(buffer.AsBuffer());
                ss.OutputStream.FlushAsync();
            }
        }
        private async Task videoFrame(StreamSocket e)
        {
            IInputStream stream =e.InputStream;
            await Task.Delay(50);
            while (true)
            {
                
                    byte[] start = { 0x48, 0x41, 0x23 };
                
                    await stream.ReadAsync(x, 1, InputStreamOptions.ReadAhead);
                    f[0] = f[1];
                    f[1] = f[2];
                    f[2] = x.ToArray()[0];
                
                
                if (f[0] == start[0] || f[1] == start[1] || f[2] == start[2])
                {
                    try
                    {
                        IBuffer i = new Windows.Storage.Streams.Buffer(4);
                        stream.ReadAsync(i, 4, InputStreamOptions.ReadAhead);
                        byte[] a = new byte[] { 0, 0, 0, 0 };
                        a = i.ToArray();
                        if(BitConverter.IsLittleEndian)
                        {
                            Array.Reverse(a);
                        }
                        sizei = BitConverter.ToUInt32(a, 0);
                        imagebuf = new Windows.Storage.Streams.Buffer(sizei);
                        InMemoryRandomAccessStream ims = new InMemoryRandomAccessStream();
                        await stream.ReadAsync(imagebuf, sizei, InputStreamOptions.ReadAhead);
                        DataWriter writer = new DataWriter(ims.GetOutputStreamAt(0));
                        writer.WriteBuffer(imagebuf);
                        await writer.StoreAsync();
                        await Windows.ApplicationModel.Core.CoreApplication.MainView.Dispatcher.RunAsync(CoreDispatcherPriority.High,()=>
                        {
                            BitmapImage image = new BitmapImage();
                            image.SetSource(ims);
                            canvas.Source=image;
                        });
                        sizei = 0;
                    }
                    catch (Exception ex)
                    {
                        Debug.WriteLine(ex.Message);
                        Debug.WriteLine(ex.StackTrace);
                    }

                }
            }
        }
               
    }
}
