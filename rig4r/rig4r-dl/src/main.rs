
mod rig4r;
use rig4r::storage::*;

fn main() {
    println!("Hello, world!");
    let mut hs = HashStore::new();
    hs.putString("foo", "store");
    println!("Store {}", hs.getString("foo").unwrap());
}

#[cfg(test)]
mod tests_main {
    #[test]
    fn test1() {
        assert_eq!(2 + 2, 4);
    }
}
