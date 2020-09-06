
mod rig4r;
use rig4r::storage::*;

fn main() {
    println!("Hello, world!");
    the_store();
   let x = HashStore::new(5);
    x.v();
}

#[cfg(test)]
mod tests_main {
    #[test]
    fn test1() {
        assert_eq!(2 + 2, 4);
    }
}
