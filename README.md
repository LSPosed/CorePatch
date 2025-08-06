# CorePatch

An Xposed Framework module that disables signature verification on Android.

![Android CI](https://github.com/coderstory/CorePatch/workflows/Android%20CI/badge.svg)
![GitHub Release (latest by date)](https://img.shields.io/github/v/release/coderstory/CorePatch)
![CRAN/METACRAN](https://img.shields.io/cran/l/devtools)

### Supported Android versions

CorePatch has been developed to support multiple Android versions through different branches.

`main` branch: Supports Android 10-15.  
`Q` branch: Supports Android 9.0-10.0.  
`master` branch: Supports Android 4.4-7.x.

Note: For Android 8.x, you may need to find the commit that changed the version name to 2.1 and compile the module yourself.

## Features

CorePatch provides several key features to bypass Android's standard installation checks.

**Downgrade apps**: Allows you to install an older version of an app, suppressing the `INSTALL_FAILED_VERSION_DOWNGRADE` error.  
**Install modified APKs**: Ignores errors like "Invalid digest", allowing you to install apps that have been modified after compilation.  
**Install with inconsistent signatures**: Overlays and installs apps even if their signatures don't match the currently installed version.

### Download

**Latest release**: You can download the latest stable version from the [GitHub Releases](https://github.com/LSPosed/CorePatch/releases) page.  
**Development builds**: Get the latest development builds directly from [GitHub Actions](https://github.com/LSPosed/CorePatch/actions).  
**Historical versions**: Older versions are available for download [here](https://soft.shouji.com.cn/down/32512.html).

### Credits

Special thanks to the following for their contributions.
- [weishu](https://github.com/tiann): For code references.
- [LSPosed](https://github.com/LSPosed/LSPosed): For the ART Hook Framework.
- [yujincheng08](https://github.com/yujincheng08): For technical support.

### Community & support

Join the community on [Telegram](https://t.me/core_patch_chat) to discuss the module and get support.  
If you find this project useful, consider supporting the developer via [PayPal](https://www.paypal.com/paypalme/code620).

### License

CorePatch is released under the GPL V2 license. See the `LICENSE` file for more details.
