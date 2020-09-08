
mod hash_store;
pub use hash_store::HashStore;
pub use hash_store::IHashStore;


#[cfg(test)]
mod tests_storage {
    #[test]
    fn test1() {
        assert_eq!(2 + 2, 4);
    }
}
