## Troubleshooting

Below are some common problems and some ideas on how to remedy them.
Please keep in mind that we plan to continue to improve the R extension following
the release of NetLogo 6. We welcome feedback on how to improve the extension as well
as bug reports pointing us to any new problems you encounter.

### Loading R packages fails

See, for example, [this](https://beta.groups.yahoo.com/neo/groups/netlogo-users/conversations/topics/18786?reverse=1) post.

### After changing the working directory in R (e.g. with `setwd()`) NetLogo doesn't find the extension

Changing working directory in R doesn't work because it changes also Java's library path that NetLogo needs to find its extensions.
Please use absolute path to any files in R instead of changing the working directory.

### Specific error code list

* Error #01. Invalid R Home. R home is specified via the R_HOME environment variable or a properties file, but couldn't be found at the specified path. See above for how to specify R home.
* Error #02: Cannot find rJava/JRI. The R Extension was unable to locate your installation of rJava. Some steps to resolve:
  * Ensure that rJava (0.9-8 or later) is installed in R. Ensure that it's installed either system-wide or for you as a user
  * Ensure that your configuration points to the proper rJava location. If you have a `user.properties` file, ensure that `jri.home.paths` includes the path given by R when you run `system.file("jri",package="rJava")`
* Error #03: Cannot load rJava libraries. This may indicate a corrupted rJava installation. Try reinstalling rJava.
* Error #04: Error in R-Extension. This is an unknown initialization error. Ensure that you are running R 3.0.0 or later and have the rJava extension installed (version 0.9-8 or later). Please report this error to bugs@ccl.northwestern.edu or open a new issue on [the R-Extension issue tracker](https://github.com/NetLogo/R-Extension/issues).
* Error #05: There was an error setting R_HOME. Check your user.properties file to ensure that r.home specifies a valid path to the R extension. You may also be able to work around this error by setting the R_HOME environment variable. If this error persists, please report it!
* Error #06: Cannot load R libraries. This may indicate a corrupted or improperly configured R installation. If you're certain that your R installation is find, please report this as an issue.

