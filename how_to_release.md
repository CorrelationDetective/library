# Tutorial for developers: How to release library after update
1. Update the code and build
2. Make sure all the tests are passing
3. Commit to the repository
4. Update the version in the `pom.xml` file
5. Write the release notes in the project description in the `README.md` file
6. Deploy using `mvn deploy`

See https://central.sonatype.org/publish/publish-maven/#distribution-management-and-authentication for more information.
