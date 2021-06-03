/*
 * Customer Credit Rating
 * This service domain maintains and administers the bank's credit assessment for customers.
 *
 * OpenAPI spec version: 2.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.redhat.mercury.customercreditrating.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * BQExternalReportingRetrieveInputModel
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-06-03T15:34:54.132884+02:00[Europe/Madrid]")
public class BQExternalReportingRetrieveInputModel {
  @SerializedName("externalReportingRetrieveActionTaskRecord")
  private Object externalReportingRetrieveActionTaskRecord = null;

  @SerializedName("externalReportingRetrieveActionRequest")
  private String externalReportingRetrieveActionRequest = null;

  @SerializedName("externalReportingInstanceReport")
  private Object externalReportingInstanceReport = null;

  @SerializedName("externalReportingInstanceAnalysis")
  private Object externalReportingInstanceAnalysis = null;

  public BQExternalReportingRetrieveInputModel externalReportingRetrieveActionTaskRecord(Object externalReportingRetrieveActionTaskRecord) {
    this.externalReportingRetrieveActionTaskRecord = externalReportingRetrieveActionTaskRecord;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The retrieve service call consolidated processing record 
   * @return externalReportingRetrieveActionTaskRecord
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The retrieve service call consolidated processing record ")
  public Object getExternalReportingRetrieveActionTaskRecord() {
    return externalReportingRetrieveActionTaskRecord;
  }

  public void setExternalReportingRetrieveActionTaskRecord(Object externalReportingRetrieveActionTaskRecord) {
    this.externalReportingRetrieveActionTaskRecord = externalReportingRetrieveActionTaskRecord;
  }

  public BQExternalReportingRetrieveInputModel externalReportingRetrieveActionRequest(String externalReportingRetrieveActionRequest) {
    this.externalReportingRetrieveActionRequest = externalReportingRetrieveActionRequest;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Text  general-info: Details of the retrieve action service request (lists requested reports) 
   * @return externalReportingRetrieveActionRequest
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Text  general-info: Details of the retrieve action service request (lists requested reports) ")
  public String getExternalReportingRetrieveActionRequest() {
    return externalReportingRetrieveActionRequest;
  }

  public void setExternalReportingRetrieveActionRequest(String externalReportingRetrieveActionRequest) {
    this.externalReportingRetrieveActionRequest = externalReportingRetrieveActionRequest;
  }

  public BQExternalReportingRetrieveInputModel externalReportingInstanceReport(Object externalReportingInstanceReport) {
    this.externalReportingInstanceReport = externalReportingInstanceReport;
    return this;
  }

   /**
   * Get externalReportingInstanceReport
   * @return externalReportingInstanceReport
  **/
  @Schema(description = "")
  public Object getExternalReportingInstanceReport() {
    return externalReportingInstanceReport;
  }

  public void setExternalReportingInstanceReport(Object externalReportingInstanceReport) {
    this.externalReportingInstanceReport = externalReportingInstanceReport;
  }

  public BQExternalReportingRetrieveInputModel externalReportingInstanceAnalysis(Object externalReportingInstanceAnalysis) {
    this.externalReportingInstanceAnalysis = externalReportingInstanceAnalysis;
    return this;
  }

   /**
   * Get externalReportingInstanceAnalysis
   * @return externalReportingInstanceAnalysis
  **/
  @Schema(description = "")
  public Object getExternalReportingInstanceAnalysis() {
    return externalReportingInstanceAnalysis;
  }

  public void setExternalReportingInstanceAnalysis(Object externalReportingInstanceAnalysis) {
    this.externalReportingInstanceAnalysis = externalReportingInstanceAnalysis;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BQExternalReportingRetrieveInputModel bqExternalReportingRetrieveInputModel = (BQExternalReportingRetrieveInputModel) o;
    return Objects.equals(this.externalReportingRetrieveActionTaskRecord, bqExternalReportingRetrieveInputModel.externalReportingRetrieveActionTaskRecord) &&
        Objects.equals(this.externalReportingRetrieveActionRequest, bqExternalReportingRetrieveInputModel.externalReportingRetrieveActionRequest) &&
        Objects.equals(this.externalReportingInstanceReport, bqExternalReportingRetrieveInputModel.externalReportingInstanceReport) &&
        Objects.equals(this.externalReportingInstanceAnalysis, bqExternalReportingRetrieveInputModel.externalReportingInstanceAnalysis);
  }

  @Override
  public int hashCode() {
    return Objects.hash(externalReportingRetrieveActionTaskRecord, externalReportingRetrieveActionRequest, externalReportingInstanceReport, externalReportingInstanceAnalysis);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BQExternalReportingRetrieveInputModel {\n");
    
    sb.append("    externalReportingRetrieveActionTaskRecord: ").append(toIndentedString(externalReportingRetrieveActionTaskRecord)).append("\n");
    sb.append("    externalReportingRetrieveActionRequest: ").append(toIndentedString(externalReportingRetrieveActionRequest)).append("\n");
    sb.append("    externalReportingInstanceReport: ").append(toIndentedString(externalReportingInstanceReport)).append("\n");
    sb.append("    externalReportingInstanceAnalysis: ").append(toIndentedString(externalReportingInstanceAnalysis)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
