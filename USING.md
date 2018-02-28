The R-Extension of NetLogo provides primitives to use the statistical software
R (Gnu S)  (see [the R Project website](http://www.r-project.org/))
within a NetLogo model. There are primitives to create R-Variables with values
from NetLogo variables or agents and others to evaluate commands in R with and
without return values.

## Using

To use the extension in your model, add a line to the top of your procedures tab:

```NetLogo
extensions [ r ]
```

If your model already uses other extensions, then it already has an
`extensions` line in it, so just add `r` to the list.

For more information on using NetLogo extensions, see the [Extensions Guide](http://ccl.northwestern.edu/netlogo/docs/extensions.html).

For examples of the usage of the R-Extension, models can be downloaded [from the project repository](https://github.com/NetLogo/R-Extension/tree/master/examples).
These models are installed with NetLogo in the "models" directory of the R extension.
Please note that (as of NetLogo 6.0) these models are *not* included in the NetLogo models library.

### Some Tips

#### Plotting

If you want to use the plot function of R, you could activate the JavaGD plot device via
[r:setPlotDevice](#rsetplotdevice), see the "plot-example1.nlogo" model. This is the prefered method!

But you can also use the standard R device, but then, you have to give R some cpu time, e.g.
by run an evalulation of `sys.sleep(0.01)` with a forever button. See the
"plot-example2.nlogo". (Many thanks to Thomas Petzold!). The creation of plots into
files is also possible. See the "plot-into-file-example.nlogo" in the examples folder.

#### Load and Save data from/into file(s)

It's possible to load and save data from file directly in R. This code snippet illustrates:

```NetLogo
r:eval "dataname <- read.table('<path to file>')" ; read file
r:eval "write.table(dataname, file='<filename>')" ; write file
```

#### Data.frame with vector in cells

Normally, a data.frame cell contains only a single value. Each column is represented as a
vector and if you would put a vector of vectors to a data.frame, it would be splitted into
several columns. With the R-Extension it is possible to put a vector into a data.frame cell,
when you assign a NetLogo List to a column which contains nested NetLogo Lists for each row.
If you want, for example, to use write.table on this data.frame, you have to mark this
column as `class="AsIs"`. You can do this by using the `I(x)`-function.

Example: If the column of interest has the name "col1" of the data.frame "df1" you could
execute `r:eval "df1$col1 <- I(df1$col1)"`. Call `help(I)` from within an R
terminal for further details.

#### Load an R-Script

Furthermore, you can define functions in an R-Script, load it, and use the functions.
Load R-files via `r:eval "source('<path to r-file>')"`.

#### Load a Package

It's also possible to load R packages via `r:eval "library(<name of package>)"`.


When you compile your code containing `extensions [r]` you will create a new R
workspace. Until you reload the extension, open a new model or submit the primitive
`[r:clear](#rclear)`, all R variables assigned in this session will be available like you would
use R from the command line or in the R Console.

#### Interactive Shell

You can open an Interactive R Shell via [`r:interactiveShell`](#rinteractiveshell). This shell is a port to
the underlaying R instance. This shell works on the global environment (see [Environments in the R Extension](#environments-in-the-r-extension) below) while the extension itself work on a custom local
environment. But there is one automatic variable "nl.env" in the global environment, which
is a reference to the local environment of the extension. Don't delete this variable!<br />
You can access a variable created by the extension via
`get("<variable name>",nl.env)`, for example `myvar <- get("myvar",nl.env)`.
If you want to plot from the Interactive Shell you should use the included JavaGD plot
device (see [r:setPlotDevice](#rsetplotdevice)).
You can save and load the history of entered R commands via a right-mouse button context
menu.

Please read the notes at the top of the output text area after opening the shell!
On Linux OS it can happen that you see an error message from X11. Please check, if
everything worked correcly. If so, you can ignore these messages. If not, please write a
report to bugs@ccl.northwestern.edu or [open an issue](https://github.com/NetLogo/R-Extension/issues).

#### Environments In the R Extension

When you load a model the R-Extension creates a new R environment. When you create an R
variable using the R-Extension, this variable is created in the local R environment.
Furthermore, all calls from the R-Extension work on this local environment. This new
environment concept enables you to use the extension in BehaviorSpace Experiments.
Therefore, you don't have to care about the environment while you're not using the
Interactive Shell or other tools, which work on the global environment. You can explicitly
assign a variable to the global environment by using the `<-` operator or by executing
`assign(<name>,<value>,envir=.GlobalEnv)`. If you work with the Interactive Shell,
see the notes at the top of the output text area after opening the shell.

Type `help(environment)` in an R shell to learn more about environments.

You can/should clear (i.e. remove all variable and free memory) the local environment via
[`r:clearLocal`](#rclearLocal). If you want to clear also the global environment (the whole workspace), call
[`r:clear`](#rclear).

#### Memory

With the R-Extension you can load R into the process of NetLogo. Because of the architecture of R, both software share one system process and therefore the memory given to NetLogo.

In some circumstances it can happen that you receive an out of memory error due to Java's heap space. You can increase the heap space before starting NetLogo by adapting the `-Xmx` JVM-parameter (see also [the NetLogo manual section on Windows memory](http://ccl.northwestern.edu/netlogo/docs/faq.html#when-i-try-to-start-netlogo-on-windows-i-get-an-error-the-jvm-could-not-be-started-help)). But on 32-bit systems, this is very limited. Therefore, it is a good idea to use a 64-bit system if you want/need to use high amount of RAM.
You can see the memory usage of R by starting the interactive shell (`r:interactiveShell`) and type there:
`memory.size(max=F)` and `memory.size(max=T)`. Furthermore, you can check the memory limit by typing: `memory.limit()`.<br />
See also:

* R manual page for [memory.profile](http://stat.ethz.ch/R-manual/R-patched/library/base/html/memory.profile.html)
* R manual page for [object.size](http://stat.ethz.ch/R-manual/R-patched/library/utils/html/object.size.html)
* R manual page for [memory.size](http://stat.ethz.ch/R-manual/R-devel/library/utils/html/memory.size.html)

If you call the garbage collector in the interactive shell by typing gc(), you will get some information about
the current memory usage (see also [http://stat.ethz.ch/R-manual/R-patched/library/base/html/gc.html](http://stat.ethz.ch/R-manual/R-patched/library/base/html/gc.html)).

If you type `gc(nl.env)` you will see the percentage of memory used for cons cells and vectors.

Don't forget to call the `r:gc` primitive after removing an R variable and don't forget to remove R variable you don't need anymore! See how the memory usage changes after removing variable and calling `r:gc`.

If you use too much memory, it can happen, that NetLogo will close abruptly. In such a case, check if there is a way to reduce the memory used. If not, try to switch over to the [Rserve-extension](http://rserve-ext.sourceforge.net/). With the Rserve-Extension both software, NetLogo and R, run independently. There is, of cause, also a limit of transferable data amount with one request, but it is less restrictive.<br />

One last note to this topic: Keep in mind that R is a vector-oriented language. Prevent mass calls with single values whenever possible and replace them by vector operations. This is much faster and more stable.

#### Headless

Since R-Extension version 1.1 it is possible use the extension when NetLogo is running in headless mode.
This is for example the case, when you run BehaviorSpace experiments from the command line (see [here](http://ccl.northwestern.edu/netlogo/docs/behaviorspace.html#advanced-usage)).
The difference is, that the `interactiveShell` is not initialized/instanciated.
You can use the extension as you know it from GUI mode, but it is not possible to open the interactiveShell ([`r:interactiveShell`](#rinteractiveshell)) and to set the plot device ([`r:setPlotDevice`](#rsetplotdevice)).
But one additional things has to be done: You have to call [`r:stop`](#rstop) finally when running NetLogo headless to stop the R engine. Otherwise NetLogo will not be closed and you will not get back to the command line prompt.
When setting up a BehaviorSpace experiment, there is the option to set final commands.
This is a good place to add the `r:stop` command (see image).

![Put r:stop in the behaviorspace final commands](images/rstop.jpg)

