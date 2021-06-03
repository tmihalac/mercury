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
 * CRCustomerCreditRatingStateRequestOutputModel
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-06-03T15:34:54.132884+02:00[Europe/Madrid]")
public class CRCustomerCreditRatingStateRequestOutputModel {
  @SerializedName("customerCreditRatingStateInstanceRecord")
  private Object customerCreditRatingStateInstanceRecord = null;

  @SerializedName("customerCreditRatingStateRequestActionTaskReference")
  private String customerCreditRatingStateRequestActionTaskReference = null;

  @SerializedName("customerCreditRatingStateRequestActionTaskRecord")
  private Object customerCreditRatingStateRequestActionTaskRecord = null;

  @SerializedName("requestRecordReference")
  private String requestRecordReference = null;

  @SerializedName("requestResponseRecord")
  private Object requestResponseRecord = null;

  public CRCustomerCreditRatingStateRequestOutputModel customerCreditRatingStateInstanceRecord(Object customerCreditRatingStateInstanceRecord) {
    this.customerCreditRatingStateInstanceRecord = customerCreditRatingStateInstanceRecord;
    return this;
  }

   /**
   * Get customerCreditRatingStateInstanceRecord
   * @return customerCreditRatingStateInstanceRecord
  **/
  @Schema(description = "")
  public Object getCustomerCreditRatingStateInstanceRecord() {
    return customerCreditRatingStateInstanceRecord;
  }

  public void setCustomerCreditRatingStateInstanceRecord(Object customerCreditRatingStateInstanceRecord) {
    this.customerCreditRatingStateInstanceRecord = customerCreditRatingStateInstanceRecord;
  }

  public CRCustomerCreditRatingStateRequestOutputModel customerCreditRatingStateRequestActionTaskReference(String customerCreditRatingStateRequestActionTaskReference) {
    this.customerCreditRatingStateRequestActionTaskReference = customerCreditRatingStateRequestActionTaskReference;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::ISO20022andUNCEFACT::Identifier  general-info: Reference to a Customer Credit Rating State instance request service call 
   * @return customerCreditRatingStateRequestActionTaskReference
  **/
  @Schema(example = "CCRSRATR731260", description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::ISO20022andUNCEFACT::Identifier  general-info: Reference to a Customer Credit Rating State instance request service call ")
  public String getCustomerCreditRatingStateRequestActionTaskReference() {
    return customerCreditRatingStateRequestActionTaskReference;
  }

  public void setCustomerCreditRatingStateRequestActionTaskReference(String customerCreditRatingStateRequestActionTaskReference) {
    this.customerCreditRatingStateRequestActionTaskReference = customerCreditRatingStateRequestActionTaskReference;
  }

  public CRCustomerCreditRatingStateRequestOutputModel customerCreditRatingStateRequestActionTaskRecord(Object customerCreditRatingStateRequestActionTaskRecord) {
    this.customerCreditRatingStateRequestActionTaskRecord = customerCreditRatingStateRequestActionTaskRecord;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The request service call consolidated processing record 
   * @return customerCreditRatingStateRequestActionTaskRecord
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The request service call consolidated processing record ")
  public Object getCustomerCreditRatingStateRequestActionTaskRecord() {
    return customerCreditRatingStateRequestActionTaskRecord;
  }

  public void setCustomerCreditRatingStateRequestActionTaskRecord(Object customerCreditRatingStateRequestActionTaskRecord) {
    this.customerCreditRatingStateRequestActionTaskRecord = customerCreditRatingStateRequestActionTaskRecord;
  }

  public CRCustomerCreditRatingStateRequestOutputModel requestRecordReference(String requestRecordReference) {
    this.requestRecordReference = requestRecordReference;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::ISO20022andUNCEFACT::Identifier  general-info: Reference to the service request record 
   * @return requestRecordReference
  **/
  @Schema(example = "RRR709488", description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::ISO20022andUNCEFACT::Identifier  general-info: Reference to the service request record ")
  public String getRequestRecordReference() {
    return requestRecordReference;
  }

  public void setRequestRecordReference(String requestRecordReference) {
    this.requestRecordReference = requestRecordReference;
  }

  public CRCustomerCreditRatingStateRequestOutputModel requestResponseRecord(Object requestResponseRecord) {
    this.requestResponseRecord = requestResponseRecord;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: Details of the request action service response 
   * @return requestResponseRecord
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: Details of the request action service response ")
  public Object getRequestResponseRecord() {
    return requestResponseRecord;
  }

  public void setRequestResponseRecord(Object requestResponseRecord) {
    this.requestResponseRecord = requestResponseRecord;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CRCustomerCreditRatingStateRequestOutputModel crCustomerCreditRatingStateRequestOutputModel = (CRCustomerCreditRatingStateRequestOutputModel) o;
    return Objects.equals(this.customerCreditRatingStateInstanceRecord, crCustomerCreditRatingStateRequestOutputModel.customerCreditRatingStateInstanceRecord) &&
        Objects.equals(this.customerCreditRatingStateRequestActionTaskReference, crCustomerCreditRatingStateRequestOutputModel.customerCreditRatingStateRequestActionTaskReference) &&
        Objects.equals(this.customerCreditRatingStateRequestActionTaskRecord, crCustomerCreditRatingStateRequestOutputModel.customerCreditRatingStateRequestActionTaskRecord) &&
        Objects.equals(this.requestRecordReference, crCustomerCreditRatingStateRequestOutputModel.requestRecordReference) &&
        Objects.equals(this.requestResponseRecord, crCustomerCreditRatingStateRequestOutputModel.requestResponseRecord);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerCreditRatingStateInstanceRecord, customerCreditRatingStateRequestActionTaskReference, customerCreditRatingStateRequestActionTaskRecord, requestRecordReference, requestResponseRecord);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CRCustomerCreditRatingStateRequestOutputModel {\n");
    
    sb.append("    customerCreditRatingStateInstanceRecord: ").append(toIndentedString(customerCreditRatingStateInstanceRecord)).append("\n");
    sb.append("    customerCreditRatingStateRequestActionTaskReference: ").append(toIndentedString(customerCreditRatingStateRequestActionTaskReference)).append("\n");
    sb.append("    customerCreditRatingStateRequestActionTaskRecord: ").append(toIndentedString(customerCreditRatingStateRequestActionTaskRecord)).append("\n");
    sb.append("    requestRecordReference: ").append(toIndentedString(requestRecordReference)).append("\n");
    sb.append("    requestResponseRecord: ").append(toIndentedString(requestResponseRecord)).append("\n");
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
