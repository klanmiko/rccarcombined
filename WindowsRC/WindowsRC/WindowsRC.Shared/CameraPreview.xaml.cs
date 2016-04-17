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
using System.Threading.Tasks;         // Used to implement asynchronous methods
using Windows.Devices.Enumeration;    // Used to enumerate cameras on the device
using Windows.Devices.Sensors;        // Orientation sensor is used to rotate the camera preview
using Windows.Graphics.Display;       // Used to determine the display orientation
using Windows.Graphics.Imaging;       // Used for encoding captured images
using Windows.Media;                  // Provides SystemMediaTransportControls
using Windows.Media.Capture;          // MediaCapture APIs
using Windows.Media.MediaProperties;  // Used for photo and video encoding
using Windows.Storage;                // General file I/O
using Windows.Storage.FileProperties; // Used for image file encoding
using Windows.Storage.Streams;        // General file I/O
using Windows.System.Display;         // Used to keep the screen awake during preview and capture
using Windows.UI.Core;                // Used for updating UI from within async operations
using Windows.Networking.Sockets;
using System.Diagnostics;
using Windows.UI.Xaml.Media.Imaging;
// The Blank Page item template is documented at http://go.microsoft.com/fwlink/?LinkId=234238

namespace WindowsRC
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class CameraPreview : Page
    {
        private readonly DisplayRequest _displayRequest = new DisplayRequest();
        private MediaCapture _mediaCapture;
        private bool _isInitialized;
        private bool _isPreviewing;
        private bool _isRecording;
        private StreamSocket ss;
        private IAsyncAction connect;
        private LowLagPhotoCapture lowLagCaptureMgr;
        private readonly DisplayInformation _displayInformation = DisplayInformation.GetForCurrentView();
        private DisplayOrientations _displayOrientation = DisplayOrientations.Portrait;
        private static readonly Guid RotationKey = new Guid("C380465D-2271-428C-9B83-ECEA3B4A85C1");
        private readonly SimpleOrientationSensor _orientationSensor = SimpleOrientationSensor.GetDefault();
        private SimpleOrientation _deviceOrientation = SimpleOrientation.NotRotated;
        private void RegisterOrientationEventHandlers()
        {
            // If there is an orientation sensor present on the device, register for notifications
            if (_orientationSensor != null)
            {
                _orientationSensor.OrientationChanged += OrientationSensor_OrientationChanged;
                _deviceOrientation = _orientationSensor.GetCurrentOrientation();
            }
            _displayInformation.OrientationChanged += DisplayInformation_OrientationChanged;
            _displayOrientation = _displayInformation.CurrentOrientation;


        }
        private void UnregisterOrientationEventHandlers()
        {
            if (_orientationSensor != null)
            {
                _orientationSensor.OrientationChanged -= OrientationSensor_OrientationChanged;
            }

            _displayInformation.OrientationChanged -= DisplayInformation_OrientationChanged;
        }
        private void OrientationSensor_OrientationChanged(SimpleOrientationSensor sender, SimpleOrientationSensorOrientationChangedEventArgs args)
        {
            if (args.Orientation != SimpleOrientation.Faceup && args.Orientation != SimpleOrientation.Facedown)
            {
                _deviceOrientation = args.Orientation;
            }
            
        }
        private async void DisplayInformation_OrientationChanged(DisplayInformation sender, object args)
        {
            _displayOrientation = sender.CurrentOrientation;

            if (_isPreviewing)
            {
                await SetPreviewRotationAsync();
            }

        }
        private async Task SetPreviewRotationAsync()
        {
            // Only need to update the orientation if the camera is mounted on the device

            // Populate orientation variables with the current state
            _displayOrientation = _displayInformation.CurrentOrientation;

            // Calculate which way and how far to rotate the preview
            int rotationDegrees = ConvertDisplayOrientationToDegrees(_displayOrientation);

            // The rotation direction needs to be inverted if the preview is being mirrored
            // Add rotation metadata to the preview stream to make sure the aspect ratio / dimensions match when rendering and getting preview frames
            var props = _mediaCapture.VideoDeviceController.GetMediaStreamProperties(MediaStreamType.VideoPreview);
            props.Properties.Add(RotationKey, rotationDegrees);
            await _mediaCapture.SetEncodingPropertiesAsync(MediaStreamType.VideoPreview, props, null);

        }
        private static int ConvertDisplayOrientationToDegrees(DisplayOrientations orientation)
        {
            switch (orientation)
            {
                case DisplayOrientations.Portrait:
                    return 90;
                case DisplayOrientations.LandscapeFlipped:
                    return 180;
                case DisplayOrientations.PortraitFlipped:
                    return 270;
                case DisplayOrientations.Landscape:
                default:
                    return 0;
            }
        }
        private static int ConvertDeviceOrientationToDegrees(SimpleOrientation orientation)
        {
            switch (orientation)
            {
                case SimpleOrientation.Rotated90DegreesCounterclockwise:
                    return 90;
                case SimpleOrientation.Rotated180DegreesCounterclockwise:
                    return 180;
                case SimpleOrientation.Rotated270DegreesCounterclockwise:
                    return 270;
                case SimpleOrientation.NotRotated:
                default:
                    return 0;
            }
        }

        public CameraPreview()
        {
            this.InitializeComponent();
        }
        private async Task StartPreview()
        {
            _displayRequest.RequestActive();
            preview.Source = _mediaCapture;
            try
            {
                await _mediaCapture.StartPreviewAsync();
                _isPreviewing = true;
                ImageEncodingProperties imgFormat = ImageEncodingProperties.CreateJpeg();

                // Create LowLagPhotoCapture object
                lowLagCaptureMgr = await _mediaCapture.PrepareLowLagPhotoCaptureAsync(imgFormat);
            }
            catch (Exception ex)
            {
                Debug.WriteLine(ex.StackTrace);
            }
        }
        private async Task InitCamera()
        {
            var cameras = await DeviceInformation.FindAllAsync();
            DeviceInformation camera = cameras.FirstOrDefault(x => x.EnclosureLocation != null && x.EnclosureLocation.Panel == Windows.Devices.Enumeration.Panel.Back);
            camera = camera ?? cameras.FirstOrDefault();
            _mediaCapture = new MediaCapture();
            var mediaInitSettings = new MediaCaptureInitializationSettings { VideoDeviceId = camera.Id };
            RegisterOrientationEventHandlers();
            SetPreviewRotationAsync();
            try
            {
                await _mediaCapture.InitializeAsync(mediaInitSettings);
                _mediaCapture.VideoDeviceController.LowLagPhoto.ThumbnailEnabled = false;
                _isInitialized = true;
            }
            catch (Exception ex)
            {
                Debug.WriteLine(ex.StackTrace);
            }

            // If initialization succeeded, start the preview
            if (_isInitialized)
            {
                await StartPreview();
                await Task.Run(()=>sendImage());
            }
        }
        private void limitreached()
        {

        }
        private async Task sendImage()
        {
            while (true)
            {
                IOutputStream os = ss.OutputStream;
                
                DataWriter writer = new DataWriter(os);
                writer.WriteBytes(new byte[] { 0x48, 0x41, 0x23 });
                CapturedPhoto cp = await lowLagCaptureMgr.CaptureAsync();
                uint length = Convert.ToUInt32(cp.Frame.Size);
                byte[] a = BitConverter.GetBytes(length);
                if (BitConverter.IsLittleEndian)
                {
                    Array.Reverse(a);
                }
                writer.WriteBytes(a);
                IBuffer temp = new Windows.Storage.Streams.Buffer(length);
                await cp.Frame.ReadAsync(temp, length, InputStreamOptions.ReadAhead);
                writer.WriteBuffer(temp);
                await writer.FlushAsync();
            }
        }
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            string ip = e.Parameter as string;
            InitCamera();
            ss = new StreamSocket();
            ss.Control.QualityOfService = SocketQualityOfService.LowLatency;
            connect = ss.ConnectAsync(new Windows.Networking.HostName(ip), "56469");
        }
    }
}
