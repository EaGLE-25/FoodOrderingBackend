package com.upgrad.FoodOrderingApp.api.controller;

import com.upgrad.FoodOrderingApp.api.model.LoginResponse;
import com.upgrad.FoodOrderingApp.api.model.LogoutResponse;
import com.upgrad.FoodOrderingApp.api.model.SignupCustomerRequest;
import com.upgrad.FoodOrderingApp.api.model.SignupCustomerResponse;
import com.upgrad.FoodOrderingApp.service.businness.CustomerService;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import com.upgrad.FoodOrderingApp.service.exception.AuthenticationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.AuthorizationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.SignUpRestrictedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/")
@CrossOrigin
public class CustomerController {

    @Autowired private CustomerService customerService;

    /**
     * Method for signing up a new user.
     * An object of 'SignupUserRequest' is received as an argument with corresponding fields.
     *
     * @return SignupUserResponse - UUID of the new user created.
     * @throws SignUpRestrictedException - Thrown if the username of email already exists in the DB.
     */
    @RequestMapping(
            method = RequestMethod.POST,
            path = "/customer/signup",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SignupCustomerResponse> signup(SignupCustomerRequest signupCustomerRequest)
            throws SignUpRestrictedException {

        CustomerEntity customerEntity = createNewUserEntity(signupCustomerRequest);
        CustomerEntity createdCustomerEntity = customerService.saveCustomer(customerEntity);
        SignupCustomerResponse customerResponse =
                new SignupCustomerResponse()
                        .id(createdCustomerEntity.getUuid())
                        .status("CUSTOMER SUCCESSFULLY REGISTERED");
        return new ResponseEntity<>(customerResponse, HttpStatus.CREATED);
    }

    /**
     * This method takes a authorization string as parameter, decodes and generates an access token.
     *
     * @param authorization "Basic <Base 64 Encoded username:password>"
     * @return SigninResponse containing user id and a access-token.
     * @throws AuthenticationFailedException Throws the error code ATH-001 if username doesn't exist,
     * ATH-002 in case of incorrect password
     */
    @RequestMapping(
            method = RequestMethod.POST,
            path = "/customer/login",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<LoginResponse> login(
            @RequestHeader("authorization") final String authorization)
            throws AuthenticationFailedException {
        byte[] decode = Base64.getDecoder().decode(authorization.split("Basic ")[1]);
        String decodedText = new String(decode);
        String[] decodedArray = decodedText.split(":");
        if(decodedArray.length!=2){
            throw new AuthenticationFailedException("ATH-003", "Incorrect format of decoded customer name and password");
        }
        CustomerAuthEntity customerAuthEntity = customerService.authenticate(decodedArray[0],decodedArray[1]);

        HttpHeaders headers = new HttpHeaders();
        headers.add("access-token", customerAuthEntity.getAccessToken());

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setId(customerAuthEntity.getCustomer().getUuid());
        loginResponse.setMessage("LOGGED IN SUCCESSFULLY");

        return new ResponseEntity<>(loginResponse, headers, HttpStatus.OK);
    }

    /**
     * This method takes an bearerToken to validate and sign the user out.
     *
     * @param bearerToken Token used for authenticating the user.
     * @return UUID of the user who is signed out.
     * @throws AuthorizationFailedException if the bearerToken is invalid.
     */
    @RequestMapping(
            method = RequestMethod.POST,
            path = "/customer/logout",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<LogoutResponse> logout(
            @RequestHeader("authorization") final String bearerToken) throws AuthorizationFailedException {
        CustomerAuthEntity customerAuthEntity = customerService.logout(bearerToken.split("Bearer ")[1]);
        CustomerEntity customerEntity = customerAuthEntity.getCustomer();
        LogoutResponse signoutResponse =
                new LogoutResponse().id(customerEntity.getUuid()).message("LOGGED OUT SUCCESSFULLY");
        return new ResponseEntity<>(signoutResponse, HttpStatus.OK);
    }

    /**
     * This method take the Sign up user request and creates a user entity.
     *
     * @param signupUserRequest
     * @return
     */
    public CustomerEntity createNewUserEntity(SignupCustomerRequest signupUserRequest){
        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setFirstName(signupUserRequest.getFirstName());
        customerEntity.setLastName(signupUserRequest.getLastName());
        customerEntity.setEmail(signupUserRequest.getEmailAddress());
        customerEntity.setContactNumber(signupUserRequest.getContactNumber());
        customerEntity.setPassword(signupUserRequest.getPassword());
        customerEntity.setUuid(UUID.randomUUID().toString());
        return customerEntity;
    }
}