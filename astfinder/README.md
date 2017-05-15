Java project to navigate through generated Java ASTs, using Eclipse JDT. Currently, it intercepts nodes representing methods and references to methods.

Although the projects has implementations using https://github.com/wala/WALA and http://asm.ow2.org/, 
the main implementation is the one based on Eclipse JDT of the class MethodReferencesFinderAST.

To have it working, import as "Existing project into Workspace", and just add the jars of the "dependencies" folder. 
Ignore the compilation problems of the other classes.

