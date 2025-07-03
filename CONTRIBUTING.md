# Contributing

## Linting
To ensure a consistent code style, [ktlint](https://pinterest.github.io/ktlint/latest/) is used together with the [ktlint Gradle plugin](https://github.com/jlleitschuh/ktlint-gradle). Run the following command to verify the code format:
```bash
./gradlew ktlintCheck
```
To fix formatting errors, run the following command:
```bash
./gradlew ktlintFormat
```

## Binary compatibility
To ensure binary compatibility and avoid unwanted changes in the public API, [gradle-binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator) is used. Run the following command to verify binary compatibility:
```bash
./gradlew apiCheck
```
If the public API was changed on purpose, you will need to dump and commit the API changes:
```bash
./gradlew apiDump
```

## Testing
Remember to add tests for your changes if possible. Run the player tests by:
```bash
./gradlew mediatailor:testDebugUnitTest
```
This project uses Describe style unit tests with Kotest: https://kotest.io/docs/framework/testing-styles.html#describe-spec

Assertions are done with Strikt: https://strikt.io/

The structure of the tests is as follows:
```kotlin
describe("some feature") {
    describe("when some condition") {
        it("should do something") {
            // test code here
            expect(someCondition).toBeTrue()
        }
    }
}
```

## Issues
For bugs and problems, please try to describe the issue as detailed as possible to help us reproduce it.

## Pull request process
> [!NOTE]  
> Before dedicating significant time to your contribution, we strongly recommend reaching out to the Bitmovin support team to confirm that your work aligns with our roadmap and contribution guidelines.

Pull requests are welcome and pair well with bug reports and feature requests. Here are some tips to follow before submitting your first PR:

- Fork the repository to your own account if you haven't already.
- Stay consistent with the file formats of this project.
- Develop in a fix or feature branch (`fix/describe-your-fix`, `feature/describe-your-feature`), not in `main` or `develop`.
- Make your changes in your fork.
- Add unit tests for your changes, if possible.
- Add an entry to the [CHANGELOG.md](CHANGELOG.md) file in the `[Unreleased]` section to describe the changes to the project.
- Submit a pull request to the main repository.
- Verify that the pull request checks pass successfully.

The versioning scheme we use is [SemVer](http://semver.org/).

All additions, modifications, and fixes that are submitted will be reviewed. The project owners reserve the right to reject any pull request that does not meet our standards. We may not be able to respond to all pull requests immediately and provide no timeframes for doing so.
