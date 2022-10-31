@ECHO OFF

:: Execute this script to check your repository using cargo check,
:: fmt and clippy. Also runs tests. This is also done in the Github
:: Actions CI. We recommend that you run this before contributing.
:: Make sure you have rustfmt and clippy (nightly version) installed:
:: rustup +nightly component add rustfmt clippy

ECHO Checking with cargo check
cargo +nightly check --manifest-path rust/Cargo.toml
ECHO Testing with cargo test
cargo +nightly test --manifest-path rust/Cargo.toml
ECHO Checking with cargo fmt
cargo +nightly fmt --manifest-path rust/Cargo.toml --all --check
ECHO Checking with cargo clippy
cargo +nightly clippy --manifest-path rust/Cargo.toml -- -D warnings
