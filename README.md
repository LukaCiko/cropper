# Cropper

Image cropping library for Android.

Cropper is a simple and customizable image cropping library for Android. It has an modern design, supports image rotation and two-finger resizing. Cropper supports all versions from Froyo.

## Download

Coming soon!

## How do I use Cropper?

Cropper provides a convenient fluent interface with which you can specify optional parameter. Use Cropper by calling `crop` method.

``` java
Cropper.crop(context, imageSourceUri, savePathUri)
    .aspectRatio(16, 9)
    .circleCrop(true)
    .start(context);
```

An image cropping activity will be started. Get the cropped bitmap by overriding `onActivityResult` method, checking if request code is `Cropper.CROP` and getting the `Cropper.SAVE_PATH` String extra.

Cropper also provides an `pick` convenience method which starts an image picking activity. Here's an example of `onActivityResult` method when using `pick` method to pick the image and `crop` method to crop it afterwards:

``` java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data)
{
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == Cropper.PICK && data != null)
    {
        Uri uri = data.getData();

        File file = new File(getExternalFilesDir(null), "cropped_image.png");

        Cropper.crop(getApplicationContext(), uri, Uri.fromFile(file)).start(this);
    }
    else if (requestCode == Cropper.CROP && data != null)
    {
        String croppedBitmapPath = data.getStringExtra(Cropper.SAVE_PATH);
        // Get cropped bitmap by running BitmapFactory.decodeFile(croppedBitmapPath);
    }
}
```

## Thanks

Cropper is based on [Cropimage](https://github.com/biokys/cropimage) by [Jan Muller](https://github.com/biokys).

## License

    Copyright 2015 Klemen Kresnik, Luka Cindro

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
