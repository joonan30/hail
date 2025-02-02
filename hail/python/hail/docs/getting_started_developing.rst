For Software Developers
-----------------------

Hail is an open-source project. We welcome contributions to the repository. If you're interested
in contributing to Hail, you will need to build your own Hail JAR and set up the testing environment.

Requirements
~~~~~~~~~~~~

You'll need:

- `Java 8 JDK <http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html>`_
- `Spark 2.4.0 <https://www.apache.org/dyn/closer.lua/spark/spark-2.4.0/spark-2.4.0-bin-hadoop2.7.tgz>`_

  - Hail is compatible with Spark 2.2.x, 2.3.x, and 2.4.x, but it *will not*
    work with Spark 1.x.x, 2.0.x, or 2.1.x.

- Python 3.6 or later, we recommend `Anaconda for Python 3 <https://www.anaconda.com/download>`_
- A recent version of GCC or Clang. GCC version should be version 5.0 or later, LLVM version 3.4 (which is Apple LLVM version 6.0) and later should be fine as well. 


Building a Hail JAR
~~~~~~~~~~~~~~~~~~~

To build Hail from source, you will need a C++ compiler and lz4 Debian users
might try::

    sudo apt-get install g++ liblz4-dev

On Mac OS X, you might try::

    xcode-select --install
    brew install lz4

The Hail source code is hosted `on GitHub <https://github.com/hail-is/hail>`_::

    git clone https://github.com/hail-is/hail.git
    cd hail/hail

By default, hail uses pre-compiled native libraries that are compatible with
recent Mac OS X and Debian releases. If you're not using on of these OSes, you
must rebuild the native libraries from source and move them into place before
building a shadowJar::

    ./gradlew nativeLibPrebuilt

Build a Hail JAR compatible with Spark 2.4.0::

    ./gradlew -Dspark.version=2.4.0 shadowJar

Should you wish to build a Hail JAR compatible with a different Spark version (2.2.0 here)::

    ./gradlew -Dspark.version=2.2.0 shadowJar

The end user documentation encourages users to use the `releaseJar` target which
ensures that `nativeLibPrebuilt` is run before `shadowJar`.


Environment Variables and Conda Environments
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You will need to set some environment variables so that Hail can find Spark, Spark can find Hail, and Python can find Hail. Add these lines to your ``.bashrc`` or equivalent, setting ``SPARK_HOME`` to the root directory of a Spark installation and ``HAIL_HOME`` to the root of the Hail repository::

    export SPARK_HOME=/path/to/spark
    export HAIL_HOME=/path/to/hail/hail
    export PYTHONPATH="$PYTHONPATH:$HAIL_HOME/python:$SPARK_HOME/python:`echo $SPARK_HOME/python/lib/py4j*-src.zip`"
    export SPARK_CLASSPATH=$HAIL_HOME/build/libs/hail-all-spark.jar
    export PYSPARK_SUBMIT_ARGS="--conf spark.driver.extraClassPath=$SPARK_CLASSPATH --conf spark.executor.extraClassPath=$SPARK_CLASSPATH --driver-memory 8G pyspark-shell"


First, create a conda environment for hail:

.. code-block:: bash

    conda create -n hail python==3.6

Activate the environment:

.. code-block:: bash

    conda activate hail

Now the shell prompt should include the name of the environment, in this case
"hail".

Use make to install dependencies:

.. code-block:: bash

    make -C hail install-deps

Now you can import hail from a python interpreter::

    $ python
    Python 3.6.5 |Anaconda, Inc.| (default, Mar 29 2018, 13:14:23)
    [GCC 4.2.1 Compatible Clang 4.0.1 (tags/RELEASE_401/final)] on darwin
    Type "help", "copyright", "credits" or "license" for more information.

    >>> import hail as hl

    >>> hl.init() # doctest: +SKIP
    Using Spark's default log4j profile: org/apache/spark/log4j-defaults.properties
    Setting default log level to "WARN".
    To adjust logging level use sc.setLogLevel(newLevel). For SparkR, use setLogLevel(newLevel).
    Running on Apache Spark version 2.4.0
    SparkUI available at http://10.1.6.36:4041
    Welcome to
         __  __     <>__
        / /_/ /__  __/ /
       / __  / _ `/ / /
      /_/ /_/\_,_/_/_/   version devel-9f866ba
    NOTE: This is a beta version. Interfaces may change
      during the beta period. We also recommend pulling
      the latest changes weekly.

    >>>


When you are finished developing hail, disable the environment

.. code-block:: bash

    source deactivate hail

The ``requirements.txt`` files may change without warning; therefore, after
pulling new changes from a remote repository, we always recommend updating the
conda environment:

.. code-block:: bash

    make -C hail install-deps


Building the Docs
~~~~~~~~~~~~~~~~~

Within the "hail" environment, run the ``makeDocs`` gradle task:

.. code-block:: bash

    ./gradlew makeDocs

The generated docs are located at ``./build/www/docs/0.2/index.html``.


Running the tests
~~~~~~~~~~~~~~~~~

Several Hail tests have additional dependencies:

 - `PLINK 1.9 <http://www.cog-genomics.org/plink2>`_

 - `QCTOOL 1.4 <http://www.well.ox.ac.uk/~gav/qctool>`_

To execute all Hail tests, run:

.. code-block:: bash

    ./gradlew -Dspark.version=${SPARK_VERSION} -Dspark.home=${SPARK_HOME} test

Contributing
~~~~~~~~~~~~

Chat with the dev team on our `Zulip chatroom <https://hail.zulipchat.com>`_ if
you have an idea for a contribution. We can help you determine if your
project is a good candidate for merging.

Keep in mind the following principles when submitting a pull request:

- A PR should focus on a single feature. Multiple features should be split into multiple PRs.
- Before submitting your PR, you should rebase onto the latest master.
- PRs must pass all tests before being merged. See the section above on `Running the tests`_ locally.
- PRs require a review before being merged. We will assign someone from our dev team to review your PR.
- Code in PRs should be formatted according to the style in ``code_style.xml``.
  This file can be loaded into Intellij to automatically format your code.
- When you make a PR, include a short message that describes the purpose of the
  PR and any necessary context for the changes you are making.
