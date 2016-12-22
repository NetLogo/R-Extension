## Installing

The R Extension is bundled with NetLogo 6. To use it, you will need a compatible R installation and you may need to configure the extension.

### Installing R

Standard R 3 installations should work (sometimes without configuration).
As of NetLogo 6, the following operating system / R versions were tested:

* Mac OS X, R 3.3.2
* Windows 10, R 3.3.2
* Ubuntu 14.04 (64-bit), R 3.0.2

Once R is installed, you will need to install the `rJava` package.
Certain features of the R extension rely on the `JavaGD` package.

To install, start the RGui from your program list, click on the item "Packages" in the
menu bar and then on "Install Package(s)". Select your favorite server and
find "rJava", as well as "JavaGD" and/or "CommonJavaJars" (both optional) in the list of packages.

If you prefer using the console, you can install the same packages by running
the following commands in the console (and following the prompts they generate, as appropriate).

```r
install.packages("rJava")
install.packages("JavaGD") # Optional
install.packages("CommonJavaJars") # Optional
```

### Configuring the R extension

If you are using Linux or Mac OS and one of the above R versions, you may not need to
perform any further configuration.
An easy way to determine whether you need to configure the extension it to open a new NetLogo model,
add `extensions [ r ]` to the code tab and press "Check."
If you see an error, you need to configure the `R` extension.
The R extension can be configured by editing the "user.properties" file in a text editor ("user.properties" is located in the r extension directory as part of the NetLogo installation).
The following keys are used to configure the extension:

* `r.home`: Controls which installation of r is used.
* `jri.home.paths`: Controls the path to the jri subdirectory of the rJava library.

Note that you will have to exit NetLogo and restart to see configuration changes take effect,
as the configuration file is only loaded once per NetLogo instance.
See below on how to determine the appropriate values to for `r.home` and `jri.home.paths`.

#### Configuring the Windows PATH

Windows requires an additional configuration step to make the R extension fully functional.
The appropriate directory from your R installation needs to be added to your PATH.
To do this, determine where your R installation is located (here we'll use the location C:\Program Files\R\R-<version>), then follow these steps.

1. Open the System Properties dialog. You can type "Environment Variable" into Cortana or navigate there through "Control Panel" > System > "Advanced system settings".
2. Click the "Environment variables..." button in the lower right of the dialog.
3. Click the "Path" variable in the lower panel, then click the lower "Edit..." button.
4. Windows 10 allows you to choose "New" and enter a separate path. If you're using Windows 7, append the value, using a semicolon to separate it from the entry before.
  * If you're using 32-bit NetLogo, enter the location `C:\Program Files\R\R-<version>\bin\i386\`
  * If you're using 64-bit NetLogo, enter the location `C:\Program Files\R\R-<version>\bin\x64\`
5. Choose OK, and OK again
6. Log out of your user and back in or restart Windows to let the setting take affect.

Note that you will need to update this setting if you wish to upgrade the version of R used by NetLogo.

#### Notes on editing "user.properties" on Windows

"user.properties" is a newline-delimited file.
This means if it is opened in "Notepad" it will look like all the text is on a single line.
For this reason, it is recommended to open first in "WordPad" and resave before editing in Notepad.
Alternatively, if you have a full-featured text editor (like Notepad++, Vim, or Emacs) installed, you can use that to edit the file.

To reiterate a warning given in the "user.properties" file, the directory separator for Windows
must be entered in user.properties as double-backslash ("\\") or single-forward-slash ("/").

### Determining `r.home` and `jri.home.paths`

`r.home` is the path to the "R" installation directory which contains the "bin" directory.
If you're having trouble finding this, you can run `R.home(component = "home")` in R, or
`R RHOME` on the command line (if R is on your path).

```r
R.home(component = "home")
# Returns "C:/PROGRA~1/R/R-33~1.2/bin/x64" on Windows.
# Will return other results on other platforms or configurations
```

`jri.home.paths` is a list of directories to check for jri.
It's in the `jri` directory under the `rJava` library installation.
You can find the `jri` directory in the `rJava` package by running the following in R:

```r
system.file("jri", package = "rJava")
# Returns "C:/Users/username/Documents/R/win-library/3.3/rJava/jri" on Windows.
# Will return other results on other platforms or configurations
```

Take the path and edit the user.properties file, uncommenting and editing one set of `r.home` and `jri.home.paths` to match
the values obtained in R.
When you're done, the user.properties file should have the following lines (given the above results):

```text
r.home=C:/PROGRA~1/R/R-33~1.2/bin/x64
jri.home.paths=C:/Users/username/Documents/R/win-library/3.3/rJava/jri
```

Save user.properties and load a model using the R extension. You should see it start and run properly.
