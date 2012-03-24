<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output omit-xml-declaration="yes" indent="yes" />
  <xsl:param name="ideaversion" />

  <xsl:template match="node()|@*" name="identity">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="vendor">
    <xsl:call-template name="identity"/>
    <xsl:text>

  </xsl:text><idea-version since-build="{$ideaversion}"/>
  </xsl:template>
</xsl:stylesheet>