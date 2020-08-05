package org.tree_ware.server.ktor

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.ResultSet
import org.tree_ware.model.getFileReader
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

fun verifyDbContents(cqlSession: CqlSession, query: String, expectedResultsFile: String) {
    val expectedReader = getFileReader(expectedResultsFile)
    assertNotNull(expectedReader)
    val expected = expectedReader.readText()

    val result: ResultSet = cqlSession.execute(query)
    val resultWriter = StringWriter()
    result.all().forEach {
        resultWriter.write(it.formattedContents)
        resultWriter.write("\n")
    }
    val actual = resultWriter.toString()

    assertEquals(expected, actual)
}
