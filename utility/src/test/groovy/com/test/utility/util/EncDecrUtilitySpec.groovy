package com.test.utility.util

import spock.lang.Specification

class EncDecrUtilitySpec extends Specification {

    EncrDecrUtil encrDecrUtil

    def setup() {
        encrDecrUtil = new EncrDecrUtil()
    }

    def encrypt() {
        given:
        String plainText = "Test123#"

        when:
        def response = EncrDecrUtil.encrypt(EncrDecrUtil.KEY, EncrDecrUtil.INIT_VECTOR, plainText)

        then:
        response
        response != plainText
    }

    def decrypt() {
        given:
        String encText = "OcuHRISL9+GSPLRvw/LLlw=="

        when:
        def response = EncrDecrUtil.encrypt(EncrDecrUtil.KEY, EncrDecrUtil.INIT_VECTOR, encText)

        then:
        response
        response != encText
    }
}
