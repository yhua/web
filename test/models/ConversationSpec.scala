package models

import _root_.helper.TestSupport
import org.specs2.mutable.Specification

class ConversationSpec extends Specification with TestSupport {

  "Conversation" should {
    "report index and ordered in desc" in new WithApplication {

      private val index = Conversation.index(93740362L, "13408654680", None, None)

      index.size must equalTo(3)
      index(0).phone must equalTo("13408654680")
      index(0).id must equalTo(Some(3))
      index(0).id must greaterThan(index(2).id)

    }

    "report index with from" in new WithApplication {

      private val index = Conversation.index(93740362L, "13408654680", Some(1), None)

      index.size must equalTo(2)
      index(0).phone must equalTo("13408654680")
      index(0).id must equalTo(Some(3))
      index(0).id must greaterThan(index(1).id)

    }

    "report index with to" in new WithApplication {

      private val index = Conversation.index(93740362L, "13408654680", None, Some(3))

      index.size must equalTo(2)
      index(0).phone must equalTo("13408654680")
      index(0).id must equalTo(Some(2))
      index(0).id must greaterThan(index(1).id)

    }
    "report empty if from the end" in new WithApplication {

      private val index = Conversation.index(93740362L, "13408654680", Some(3), None)

      index.size must equalTo(0)
    }

    "report empty if to the beginning" in new WithApplication {

      private val index = Conversation.index(93740362L, "13408654680", None, Some(1))

      index.size must equalTo(0)
    }
  }


}
