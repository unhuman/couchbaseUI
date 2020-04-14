# couchbaseUI
Simple UI to manipulate Couchbase documents and indexes.

Note this has to be built with Java 8 and no Lambda expressions.
The reason for this is because a shaded jar is desired and using
the IntelliJ SwingUI designer, which doesn't seem to have support
for Java 11.  I really have no idea why Lambdas don't work, but 
I found a comment on the internet and removing them resolved the
issues.
