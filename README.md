# Public Good

## Description

Public Good is an app that is meant to be used to counteract issues relating to supply chain, price gouging, scarcity, and discriminatory laws occurring on a local and state level. Ideally, the app will focus on goods that may mean the difference between life and death (or the quality of life) of people in vulnerable populations.


## Project Features

- The user can search for products in limited supply, such as baby formula, at nearby WalMart and Target stores
- The user can see which products are in stock and at which stores
- The user can click on a link to be redirected to the store where they can purchase the product


## Future Enhancements

- The user can login and save items, write on the community forum, etc.
- The registered users can read and write on a community forum
- The user can see a map of the store locations
- The user can filter by in stock items, brand, and store
- The user can sort products by price
- The user can set the store search radius


## Technologies Used & Why We Chose Them

- Frontend: React.js
- Backend: Spring Boot Java
- Web Scraping: Walmart.com and Target.com

We had to rely on web scraping as we were not able to access the Walmart and Target APIs. As for our choice of backend technology, we wanted to learn a statically typed language. We also wanted to solidify our React skills.


## Project Challenges and Learning Goals
### Challenges
- Accessing APIs and retrieving necessary data
- Web scraping in Java — many resources/examples are based on Python
- Deploying

### Learnings
- Java, Spring Boot, Gradle, IntelliJ, Jira
- Project management: planning a larger project
- Working in a team
    - Non-Technical: communication, adaptability
    - Pair programming
    - Proper git hygiene, e.g. pull requests, merge conflicts, branches, etc.

## Setting Up the Project

If you’d like to make changes to the project, please fork it. Otherwise you may clone it.

cd into the folder where you’d like to clone the project:

```shell=bash
  git clone https://github.com/public-good-app/public-good-back-end-server.git 

  gradle build

```
## Running the Project Locally

Ensure that your IDE is set to version 17 of Java / OpenJDK. If not, change sourceCompatibility in build.gradle.

Click Play button next to the dropdown menu, Select Run/Debug Configuration, to run locally

Go to Port http://localhost.com/8080/api/v1/student

## Credits

Public Good is a capstone project for [Ada Developers Academy](https://adadevelopersacademy.org/) built by Ying Goings, Natascha Jenkins, Roshni Patel, and Tori Shade.

## Resources Used:

- Spring Boot Tutorial: https://www.youtube.com/watch?v=9SGDpanrc8U

- Target Web Scraping: https://www.youtube.com/watch?v=IArLJJFT6Nk 


