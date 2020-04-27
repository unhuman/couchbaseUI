# couchbaseUI
Simple UI to manipulate Couchbase documents and indexes.

Note this has to be built with Java 8 and no Lambda expressions.
The reason for this is because a shaded jar is desired and using
the IntelliJ SwingUI designer, which doesn't seem to have support
for Java 11.  I really have no idea why Lambdas don't work, but 
I found a comment on the internet and removing them resolved the
issues.

The following were used in the creation of this project:

https://github.com/codesqueak/jackson-json-crypto

If you like this, please email unhumansoftware@gmail.com to let us know!
And, if you really want to be nice, you can PayPal a donation, but it's not expected.  Better yet, let me know you've made a donation to a food bank.

Developer Notes:
Earlier there were problems with making the build work properly with Java 11 with the ideauidesigner-maven-plugin.
This also affected Lambdas from working properly, those got removed earlier.

This has been resolved by setting the IntelliJ GUI designer to output Java source code instead of Binary class files.
