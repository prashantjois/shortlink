package ca.jois.shortlink.generator

import ca.jois.shortlink.model.ShortCode

interface ShortCodeGenerator {
    fun generate(): ShortCode
}
