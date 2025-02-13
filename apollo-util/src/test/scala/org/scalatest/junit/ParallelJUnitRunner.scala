/*
 * Copyright 2001-2008 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatest.junit

import org.junit.runner.{Description, RunWith}
import org.scalatest._
import org.junit.runner.notification.RunNotifier
import tools.ConcurrentDistributor
import java.util.concurrent.{ExecutorService, Executors}
import scala.Some

/*
 I think that Stopper really should be a no-op, like it is, because the user has
 no way to stop it. This is wierd, because it will call nested suites. So the tests
 just pile up. Oh, I see, the darn information about which test it is is in the
 stupid Description displayName. We probably need to add optional test name and
 suite class name to Report, just to satisfy JUnit integration.
*/
/**
 * A JUnit <code>Runner</code> that knows how to run any ScalaTest <code>Suite</code>
 * (or <code>Spec</code>, which extends <code>Suite</code>).
 * This enables you to provide a JUnit <code>RunWith</code> annotation on any
 * ScalaTest <code>Suite</code>. Here's an example:
 *
 * <pre>
 * import org.junit.runner.RunWith
 * import org.scalatest.junit.JUnitRunner
 * import org.scalatest.FunSuite
 *
 * @RunWith( c l a s s O f[ J U n i t R u n n e r ] )
 *         class MySuite extends FunSuite {
 *         // ...
 *         }
 *         </pre>
 *
 *         <p>
 *         This <code>RunWith</code> annotation will enable the <code>MySuite</code> class
 *         to be run by JUnit 4.
 *         </p>
 *
 * @author Bill Venners
 * @author Daniel Watson
 * @author Jon-Anders Teigen
 * @author Colin Howe
 */
@RunWith(classOf[JUnitRunner])
final class ParallelJUnitRunner(suiteClass: java.lang.Class[Suite]) extends org.junit.runner.Runner {

  private val canInstantiate = Suite.checkForPublicNoArgConstructor(suiteClass)
  require(canInstantiate, "Must pass an org.scalatest.Suite with a public no-arg constructor")

  private val suiteToRun = suiteClass.newInstance

  /**
   * Get a JUnit <code>Description</code> for this ScalaTest <code>Suite</code> of tests.
   *
   * return a <code>Description</code> of this suite of tests
   */
  val getDescription = createDescription(suiteToRun)

  private def createDescription(suite: Suite): Description = {
    val description = Description.createSuiteDescription(suite.getClass)
    // If we don't add the testNames and nested suites in, we get
    // Unrooted Tests show up in Eclipse
    for (name <- suite.testNames) {
      description.addChild(Description.createTestDescription(suite.getClass, name))
    }
    for (nestedSuite <- suite.nestedSuites) {
      description.addChild(createDescription(nestedSuite))
    }
    description
  }

  /**
   * Run this <code>Suite</code> of tests, reporting results to the passed <code>RunNotifier</code>.
   * This class's implementation of this method invokes <code>run</code> on an instance of the
   * <code>suiteClass</code> <code>Class</code> passed to the primary constructor, passing
   * in a <code>Reporter</code> that forwards to the  <code>RunNotifier</code> passed to this
   * method as <code>notifier</code>.
   *
   * @param notifier the JUnit <code>RunNotifier</code> to which to report the results of executing
   *                 this suite of tests
   */
  def run(notifier: RunNotifier) {
    val reporter = new org.scalatest.junit.RunNotifierReporter(notifier)
    var stopper = new Stopper {}
    var filter = Filter()
    var configMap = Map[String, Any]()
    ParallelJUnitRunner.withExecutor { executor =>
      val distributor = new ConcurrentDistributor(reporter, stopper, filter, configMap, executor)
      try {
        suiteToRun.run(None, reporter, stopper, filter, configMap, Some(distributor), new Tracker)
      } finally {
        distributor.waitUntilDone()
      }
    }
  }

  /**
   * Returns the number of tests that are expected to run when this ScalaTest <code>Suite</code>
   * is run.
   *
   * @return the expected number of tests that will run when this suite is run
   */
  override def testCount() = suiteToRun.expectedTestCount(Filter())
}

object ParallelJUnitRunner {

  private var useCounter = 0
  private var executor:ExecutorService = _

  // We share an executor across multiple concurrent test executions.
  def withExecutor[T](fun: (ExecutorService)=>T):T= {
    val value = this.synchronized {
      useCounter+=1
      if( executor == null) {
        var threads = Integer.getInteger("test.threads", Runtime.getRuntime.availableProcessors*2)
        println("ParallelJUnitRunner using up to "+threads+" threads to execute parallel tests.")
        executor = Executors.newFixedThreadPool(threads);
      }
      executor
    }
    try {
      fun(value)
    } finally {
      val shutdown = this.synchronized {
        useCounter-=1
        if( useCounter == 0) {
          executor = null
          true
        } else {
          false
        }
      }
      if ( shutdown ) {
        value.shutdown()
      }
    }
  }

}
