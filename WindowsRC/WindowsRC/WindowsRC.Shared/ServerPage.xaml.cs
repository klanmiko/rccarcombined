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
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;
using System.Threading.Tasks;
using System.Threading;
using Windows.Networking.Sockets;
// The Blank Page item template is documented at http://go.microsoft.com/fwlink/?LinkId=234238

namespace WindowsRC
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class ServerPage : Page
    {
        private StreamSocketListener ssl;
        private StreamSocket ss;
        private CancellationTokenSource ctsVertical;
        private CancellationTokenSource ctsHorizontal;
        private void connected(StreamSocketListener listener, StreamSocketListenerConnectionReceivedEventArgs e)
        {
            ss = e.Socket;
        }
        public ServerPage()
        {
            this.InitializeComponent();
            ssl = new StreamSocketListener();
            ssl.Control.QualityOfService = SocketQualityOfService.LowLatency;
            ssl.ConnectionReceived += new TypedEventHandler<StreamSocketListener, StreamSocketListenerConnectionReceivedEventArgs>(connected);
            ssl.BindServiceNameAsync
        }
        private void smoothBreak(Slider bar, double end) {
            double start = bar.Value;
            while(!(bar.Value==end))
            {
                bar.Value=start+(end- start)*0.2;
            }
        }
        private void send_Value(object sender, RangeBaseValueChangedEventArgs e)
        {
            Slider bar = sender as Slider;
            if (bar.Name == "speed")
            {

            }
        }
    }
}
